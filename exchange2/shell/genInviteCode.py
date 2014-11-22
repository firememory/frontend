#!/usr/bin/python

import random

""" generate invote code for coinport exchange """

def main():
    hexCodeMin = 0x10000000
    hexCodeMax = 0xFFFFFFFF - 1
    inviteCode = []
    for i in range(0, 999):
        rdNum = random.randint(hexCodeMin, hexCodeMax)
        rdNumHexStr = str(hex(rdNum)).upper()[2:]
        inviteCode.append(rdNumHexStr)

    codeFile = open("../inviteCode.txt", "a+")
    for c in inviteCode:
        codeFile.write("%s\n" % c)
    codeFile.close()


if __name__ == "__main__":
    main()
