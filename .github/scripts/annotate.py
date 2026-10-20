"""Print GitHub ::error annotations for Maven compile errors and failed tests.

Usage: annotate.py <maven log> <surefire reports dir> [source root, e.g. ui-tests]
"""
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

log_path, reports_dir = sys.argv[1], sys.argv[2]
root = sys.argv[3].rstrip('/') + '/' if len(sys.argv) > 3 and sys.argv[3] else ''
cwd = os.getcwd() + '/'


def esc(s):
    return s.replace('%', '%25').replace('\r', '').replace('\n', '%0A')


try:
    for line in open(log_path, encoding='utf-8', errors='replace'):
        m = re.search(r'\[ERROR\] (/\S+?\.java):\[(\d+),(\d+)\] (.*)', line)
        if m:
            path = m[1][len(cwd):] if m[1].startswith(cwd) else m[1]
            print(f'::error file={path},line={m[2]},col={m[3]}::{esc(m[4])}')
except FileNotFoundError:
    pass

for path in glob.glob(f'{reports_dir}/TEST-*.xml'):
    for case in ET.parse(path).getroot().iter('testcase'):
        for problem in list(case.findall('failure')) + list(case.findall('error')):
            cls = case.get('classname', '')
            file = root + ('src/test/java/' + cls.replace('.', '/') + '.java' if cls.startswith('com.') else 'src/test/resources/features')
            msg = (problem.get('message') or problem.text or '').strip()[:900]
            print(f'::error file={file},title={esc(cls.split(".")[-1] + "." + case.get("name", "test"))}::{esc(msg)}')
