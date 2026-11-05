from pathlib import Path
from collections import defaultdict
import csv
import math
import re
import statistics
import subprocess
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
log = []

def passed(message):
    log.append('PASS ' + message)
    print(log[-1])

def command(arguments, cwd=ROOT):
    result = subprocess.run(arguments, cwd=cwd, capture_output=True, text=True, encoding='utf-8')
    if result.returncode:
        raise AssertionError('Command failed\n' + ' '.join(map(str, arguments)) + '\n' + result.stderr)
    return result.stdout.strip(), result.stderr.strip()

def csv_rows(path):
    with path.open(encoding='utf-8-sig', newline='') as source:
        return list(csv.DictReader(source))

all_sources = list(ROOT.glob('Task*/*.java')) + list((ROOT / 'Task03/before').glob('*.java')) + list((ROOT / 'common').glob('*.java'))
for source in all_sources:
    text = source.read_text(encoding='utf-8')
    code = re.sub(r'"(?:\\.|[^"\\])*"', '""', text)
    assert not re.search(r'//|/\*|\*/|TODO', code), source
passed(f'{len(all_sources)} Java source files contain no comments or TODO markers')

for number in range(1, 8):
    task = ROOT / f'Task{number:02}'
    output = ROOT / 'build' / 'verification' / task.name
    output.mkdir(parents=True, exist_ok=True)
    sources = [ROOT / 'common/Bench.java', *task.glob('*.java')]
    _, warnings = command(['javac', '--release', '17', '-Xlint:all', '-d', str(output), *map(str, sources)])
    compile_log = warnings + ('\n' if warnings else '') + 'Compilation successful with --release 17 and -Xlint:all.\n'
    (task / 'compilation.txt').write_text(compile_log, encoding='utf-8')
    passed(f'{task.name} freshly compiled for Java 17')

expected = {
    (3, 'Attendance'): 'Unique students: 2',
    (5, 'LeaderboardDemo'): '[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]',
    (6, 'Triage'): 'Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)',
}
for (number, program), text in expected.items():
    output, _ = command(['java', '-cp', str(ROOT / 'build/verification' / f'Task{number:02}'), program])
    assert output == text, (program, output)
    passed(f'{program} fresh execution matches the supplied expected output exactly')

before_build = ROOT / 'build/verification/before'
before_build.mkdir(parents=True, exist_ok=True)
command(['javac', '--release', '17', '-d', str(before_build), str(ROOT / 'Task03/before/Attendance.java')])
before_output, _ = command(['java', '-cp', str(before_build), 'Attendance'])
assert before_output == 'Unique students: 3', before_output
passed('Before-correction Attendance independently reproduced Unique students: 3')

timing_groups = 0
timing_trials = 0
screenshots = 0
for number in range(1, 8):
    task = ROOT / f'Task{number:02}'
    for name in ('Answer.md', 'execution.txt', 'input.properties', 'raw.csv', 'compilation.txt'):
        assert (task / name).is_file() and (task / name).stat().st_size > 0, (task, name)
    transcript = (task / 'execution.txt').read_text(encoding='utf-8-sig')
    assert 'PASS' in transcript, task
    assert 'Exception in thread' not in transcript and 'AssertionError' not in transcript, task
    lines = transcript.splitlines()
    while lines and not lines[-1].strip():
        lines.pop()
    expected_pages = math.ceil(len(lines) / 24)
    pages = sorted(task.glob('Output_Page*.png'))
    assert len(pages) >= expected_pages, (task, len(pages), expected_pages)
    for page in pages:
        with Image.open(page) as image:
            assert image.width >= 1000 and image.height >= 650, page
            image.verify()
    screenshots += len(pages)
    raw = csv_rows(task / 'raw.csv')
    if number == 3:
        assert len(raw) == 35
        for row in raw:
            assert row['predicted_size'] == row['actual_size']
        grouped = defaultdict(list)
        for row in raw:
            grouped[row['test']].append(row)
        with (task / 'summary.csv').open('w', newline='', encoding='utf-8') as destination:
            writer = csv.writer(destination)
            writer.writerow(['test', 'trials', 'predicted_size', 'observed_size', 'all_match'])
            for test, trials in grouped.items():
                assert len(trials) == 7
                writer.writerow([test, len(trials), trials[0]['predicted_size'], trials[0]['actual_size'], True])
        passed('Task03 all 35 predicted-size observations match')
        continue
    groups = defaultdict(list)
    cross_checksums = defaultdict(set)
    for row in raw:
        groups[(row['size'], row['structure'], row['operation'])].append(row)
        cross_checksums[(row['size'], row['operation'])].add(row['checksum'])
        assert int(row['total_ns']) > 0 and int(row['operations']) > 0
        assert math.isclose(int(row['total_ns']) / int(row['operations']), float(row['ns_per_op']), abs_tol=1e-5)
    for checksums in cross_checksums.values():
        assert len(checksums) == 1, (task, checksums)
    summaries = csv_rows(task / 'summary.csv')
    assert len(groups) == len(summaries)
    for summary in summaries:
        trials = groups[(summary['size'], summary['structure'], summary['operation'])]
        assert sorted(int(row['trial']) for row in trials) == list(range(1, 8))
        samples = [int(row['total_ns']) / int(row['operations']) for row in trials]
        assert math.isclose(statistics.mean(samples), float(summary['mean_ns_per_op']), abs_tol=1e-4)
        assert math.isclose(statistics.stdev(samples), float(summary['sd_ns_per_op']), abs_tol=1e-4)
        assert math.isclose(min(samples), float(summary['min_ns_per_op']), abs_tol=1e-4)
        assert math.isclose(max(samples), float(summary['max_ns_per_op']), abs_tol=1e-4)
        timing_trials += len(trials)
        timing_groups += 1
    passed(f'{task.name} raw trials, summary statistics, checksums and screenshot coverage verified')

order_checks = csv_rows(ROOT / 'Task04/order_checks.csv')
assert len(order_checks) == 9 and all(row['code_order_ok'] == 'true' and row['insertion_order_ok'] == 'true' for row in order_checks)
ranking_checks = csv_rows(ROOT / 'Task05/correctness.csv')
assert len(ranking_checks) == 7 and all(row['chain_laws_hold'] == 'true' and row['subtraction_laws_hold'] == 'false' for row in ranking_checks)
redesign = csv_rows(ROOT / 'Task07/operation_counts.csv')
for row in redesign:
    expected_sorts = 30 if row['structure'] == 'Original' or row['update_every'] == '1' else (1 if row['update_every'] == '0' else 3)
    assert int(row['sorts']) == expected_sorts
passed('Map order checks, ranking fault detection and redesign invalidation counts verified')

tracked, _ = command(['git', 'ls-files'])
assert not any(path.startswith('docs/') or path.endswith(('.docx', '.pdf')) for path in tracked.splitlines())
ignored, _ = command(['git', 'check-ignore', 'docs/Assignment2_Answers.docx', 'docs/Assignment2_Answers.pdf'])
assert len(ignored.splitlines()) == 2
passed('Local DOCX and PDF are ignored and absent from Git tracking')
passed(f'Total evidence verified: {timing_groups} timing groups, {timing_trials} measured trials, {screenshots} screenshots')
(ROOT / 'verification.txt').write_text('\n'.join(log) + '\n', encoding='utf-8')
