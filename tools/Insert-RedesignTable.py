import csv
from pathlib import Path

folder = Path(__file__).resolve().parent.parent / 'Task07'
with (folder / 'summary.csv').open() as source:
    data = {(row['size'], row['structure'], row['operation']): row for row in csv.DictReader(source)}
lines = ['| Teams | Update interval | Original ms mean ± SD | Cached ms mean ± SD | Ratio |',
         '| --- | --- | --- | --- | --- |']
for size in (100, 1000, 10000):
    for interval in (0, 10, 1):
        original = data[(str(size), 'Original', 'updateEvery' + str(interval))]
        cached = data[(str(size), 'Cached', 'updateEvery' + str(interval))]
        def cell(row):
            return f"{float(row['mean_ns_per_op']) / 1e6:.4f} ± {float(row['sd_ns_per_op']) / 1e6:.4f}"
        ratio = float(original['mean_ns_per_op']) / float(cached['mean_ns_per_op'])
        lines.append(f'| {size} | {interval} | {cell(original)} | {cell(cached)} | {ratio:.2f} |')
path = folder / 'Answer.md'
path.write_text(path.read_text(encoding='utf-8').replace('TIMING_TABLE', '\n'.join(lines)), encoding='utf-8')
