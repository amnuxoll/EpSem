import sys
import os
 
argc = len(sys.argv)
print("argc=" + str(argc))
if (argc < 2):
    print("ERROR:  Must pass port number as a parameter.")

portNum = int(sys.argv[1])
     
print("port number: ", portNum)
os.system("mousepad");
