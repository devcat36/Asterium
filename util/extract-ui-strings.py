#!/usr/bin/env python3
#
# Asterium - a touch-first planetarium, forked from Stellarium.
# Copyright (C) 2026 the Asterium authors
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Suite 500, Boston, MA  02110-1335, USA.

import json
import os
import re
import sys

IDENT = re.compile(r'''(?x)
      [a-z][A-Za-z0-9]*
    | [A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+\.?
    | [A-Za-z]*[a-z][A-Z][A-Za-z0-9_]*\.
    | b?bt[A-Z][A-Za-z0-9]*
    | action[A-Za-z0-9_]+
    | [A-Z][a-z]+(Mgr|Sheet|Page|Card|Chrome|Dialog)[A-Za-z0-9]*
''')

NOT_PROSE = {
    'Asterium', 'SolarSystem', 'PartsDisplayed', 'PartsLabeled', 'Byname',
    'AstrAlm1984', 'ExpSup1992', 'ExpSup2013', 'Mallama2018', 'Mueller1893',
    'dd-mm-yyyy', 'mm-dd-yyyy', 'HH:mm', '-off', '-on', '.png',
    '12h30m49.4s', 'Translit', 'Native', 'Modern', 'license:',
}


def literals(src):
    found = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '/' and i + 1 < n and src[i + 1] == '/':
            i = src.find('\n', i)
            if i < 0:
                break
        elif c == '/' and i + 1 < n and src[i + 1] == '*':
            end = src.find('*/', i + 2)
            i = n if end < 0 else end + 2
        elif c == "'":
            i += 1
            while i < n and src[i] != "'":
                i += 2 if src[i] == '\\' else 1
            i += 1
        elif c == '"':
            j = i + 1
            while j < n and src[j] != '"':
                j += 2 if src[j] == '\\' else 1
            found.append((i, j + 1, src[i + 1:j]))
            i = j + 1
        else:
            i += 1
    return found


def unescape(raw):
    try:
        return json.loads('"%s"' % raw.replace("\\'", "'"))
    except ValueError:
        return raw


def keys_in(src):
    found = literals(src)
    keys, run, start = [], [], 0
    for index, item in enumerate(found):
        if run and src[found[index - 1][1]:item[0]].strip() != '+':
            keys.append((''.join(run), src.count('\n', 0, start) + 1))
            run = []
        if not run:
            start = item[0]
        run.append(unescape(item[2]))
    if run:
        keys.append((''.join(run), src.count('\n', 0, start) + 1))
    return keys


FORMAT_ONLY = re.compile(r'[^A-Za-z]*(%[-+ 0-9.]*[a-zA-Z][^A-Za-z]*)+')
DATE_PATTERN = re.compile(r"[dMyHhmsEaGwWkKzZ' :/.,-]+")


def is_prose(s):
    if not re.search(r'[A-Za-z]', s) or s != s.strip() or s in NOT_PROSE:
        return False
    if '\\' in s or s.endswith('=') or re.match(r'^[+\-\u2212]\d', s) or FORMAT_ONLY.fullmatch(s):
        return False
    if (DATE_PATTERN.fullmatch(s) and re.search(r'dd|MM|yy|HH|mm|ss', s)
            and re.search(r'[-/: ]', s)):
        return False
    if sum(c.isascii() and c.isalpha() for c in s) < 0.25 * len(s):
        return False
    if '/' in s and (s.endswith('/') or '://' in s or re.search(r'\.[A-Za-z0-9]{2,4}$', s)):
        return False
    if ' ' not in s:
        if IDENT.fullmatch(s) or ('_' in s):
            return False
        if re.fullmatch(r'[A-Z_]{2,}', s) and s not in ('OBJECT', 'OBJECTS'):
            return False
    return True


def scan(source_dir):
    found = {}
    for base, _, names in os.walk(source_dir):
        for name in sorted(names):
            if not name.endswith('.java'):
                continue
            path = os.path.join(base, name)
            with open(path, encoding='utf-8') as handle:
                for key, line in keys_in(handle.read()):
                    if len(key) > 1:
                        found.setdefault(key, []).append(
                            '%s:%d' % (os.path.relpath(path), line))
    return found


TRANSLATED = re.compile(r'(?<![A-Za-z0-9_])(?:ct_|N_)\(\s*$')


def scan_engine(source_dir):
    found = {}
    for name in sorted(os.listdir(source_dir)):
        if not name.endswith('.cpp'):
            continue
        path = os.path.join(source_dir, name)
        with open(path, encoding='utf-8') as handle:
            src = handle.read()
        for start, end, _ in literals(src):
            if not TRANSLATED.search(src[max(0, start - 40):start]):
                continue
            run, at = [], start
            while True:
                run.append(unescape(src[at + 1:end - 1]))
                gap = src[end:]
                step = re.match(r'\s*\n?\s*"', gap)
                if not step:
                    break
                at = end + step.end() - 1
                closing = at + 1
                while closing < len(src) and src[closing] != '"':
                    closing += 2 if src[closing] == '\\' else 1
                end = closing + 1
            key = ''.join(run)
            if len(key) > 1:
                found.setdefault(key, []).append(
                    '%s:%d' % (os.path.relpath(path), src.count('\n', 0, start) + 1))
    return found


def quote(s):
    body = (s.replace('\\', '\\\\').replace('"', '\\"')
             .replace('\t', '\\t').replace('\n', '\\n'))
    return '"%s"' % body


def write_pot(found, out_path):
    lines = [
        '# Asterium interface translation template.',
        '# Copyright (C) 2026 the Asterium authors',
        '# This file is distributed under the same licence as the Asterium package.',
        '#',
        'msgid ""',
        'msgstr ""',
        '"Project-Id-Version: asterium\\n"',
        '"MIME-Version: 1.0\\n"',
        '"Content-Type: text/plain; charset=UTF-8\\n"',
        '"Content-Transfer-Encoding: 8bit\\n"',
        '',
    ]
    kept = 0
    for key in sorted(k for k in found if is_prose(k)):
        kept += 1
        lines.append('#: %s' % ' '.join(found[key]))
        lines.append('msgid %s' % quote(key))
        lines.append('msgstr ""')
        lines.append('')
    with open(out_path, 'w', encoding='utf-8') as handle:
        handle.write('\n'.join(lines))
    print('    %d translatable strings -> %s' % (kept, out_path))


def write_table(found, out_path):
    keys = sorted(found)
    with open(out_path, 'w', encoding='utf-8') as handle:
        json.dump(keys, handle, ensure_ascii=False, indent=0)
    print('    %d interface strings -> %s' % (len(keys), out_path))


ENGINE_DIR = 'src/gui/mobile'

if __name__ == '__main__':
    argv = sys.argv[1:]
    as_pot = '--pot' in argv
    if as_pot:
        argv.remove('--pot')
    found = scan(argv[0])
    if as_pot:
        for key, refs in scan_engine(ENGINE_DIR).items():
            found.setdefault(key, []).extend(refs)
    (write_pot if as_pot else write_table)(found, argv[1])
