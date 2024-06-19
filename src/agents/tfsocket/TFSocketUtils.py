def log(s):
    f = open('pyout.txt', 'a')
    f.write(s)
    f.write('\n')
    f.close()