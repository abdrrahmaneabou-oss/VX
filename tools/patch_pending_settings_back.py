#!/usr/bin/env python3
from pathlib import Path
import sys

def main():
    root=Path(sys.argv[1])
    print('Settings escape patch path:', root)

if __name__ == '__main__':
    main()
