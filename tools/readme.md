make-pkg.py
===========

install setuptools first

https://pypi.python.org/packages/2.7/s/setuptools/setuptools-0.6c11.win32-py2.7.exe

and install prerequisites with following instructions

    easy_install pip
    pip install plumbum

then run make-pkg.py

    python make-pkg.py -f leaf.in

leaf.in contains list of pom files of bundles should be included in package spec (line-separated)
