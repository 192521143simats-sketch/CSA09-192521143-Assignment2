import csv
from pathlib import Path

folder = Path(__file__).resolve().parent.parent / 'Task04'
with (folder / 'summary.csv').open() as source:
    rows = list(csv.DictReader(source))
data = {(row['size'], row['structure'], row['operation']): row for row in rows}

def formatted(row, factor):
    return f"{float(row['mean_ns_per_op']) * factor:.3f} ± {float(row['sd_ns_per_op']) * factor:.3f}"

with (folder / 'Answer.md').open('a', encoding='utf-8') as answer:
    answer.write('\n| Size | Structure | 100000 lookups ms | Code report ms | Addition report ms |\n')
    answer.write('| --- | --- | --- | --- | --- |\n')
    for size in (100, 10000, 100000):
        for structure in ('HashMap', 'TreeMap', 'LinkedHashMap'):
            cells = [str(size), structure]
            for operation, factor in [('100000-lookups', .1), ('code-report', 1e-6), ('insertion-report', 1e-6)]:
                cells.append(formatted(data[(str(size), structure, operation)], factor))
            answer.write('| ' + ' | '.join(cells) + ' |\n')
