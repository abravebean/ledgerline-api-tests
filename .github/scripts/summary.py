"""Write a Markdown results table from Surefire XML for the job summary."""
import glob
import sys
import xml.etree.ElementTree as ET

totals = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
rows = []
for path in sorted(glob.glob(f'{sys.argv[1]}/TEST-*.xml')):
    root = ET.parse(path).getroot()
    counts = {k: int(root.get(k, 0)) for k in totals}
    for k in totals:
        totals[k] += counts[k]
    ok = counts['failures'] + counts['errors'] == 0
    name = root.get('name', path).split('.')[-1]
    rows.append(f"| {'✅' if ok else '❌'} | {name} | {counts['tests']} | {counts['failures'] + counts['errors']} | {float(root.get('time', 0)):.1f}s |")

failed = totals['failures'] + totals['errors']
print('## Ledgerline API tests\n')
print(f"**{totals['tests'] - failed - totals['skipped']} passed**, {failed} failed, {totals['skipped']} skipped\n")
print('| | Suite | Tests | Failed | Time |\n|---|---|---|---|---|')
print('\n'.join(rows) or '| | No results | | | |')
print('\nThe Cucumber HTML report is in the `test-reports` artifact.')

# Also as an annotation, so the counts show on the run page without opening the summary.
notice = f"{totals['tests'] - failed - totals['skipped']} passed, {failed} failed, {totals['skipped']} skipped"
print(f'::notice title=Ledgerline API tests::{notice}', file=sys.stderr)
