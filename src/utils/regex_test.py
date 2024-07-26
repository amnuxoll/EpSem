import re

var = 3
history = 'aaaaabbAbbabbababaaaaabbaaabaabbabbbabaaababababababaabbbbbaabababbaaabaabbbaaaaabbbAbbbAbbbbbbabbBbabbAbbababbaabaabbBbaaaabbbbbabbBaabbAbbAbaaaabbababbbabbA'
pattern = rf'([a-z]*[A-Z]){{{var}}}(?=[a-z]*\Z)'
match = re.search(pattern, history)
substring = match.group(0) if match else ''
print(substring)