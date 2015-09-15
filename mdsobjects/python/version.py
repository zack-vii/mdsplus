"""
This is a helper module.
Its purpose is to supply tools that are used to generate version specific code.
Goal is to generate code that work on both python2x and python3x.
"""
from numpy import generic as npscalar
from numpy import ndarray as nparray
from sys import version_info as pyver
ispy3 = pyver>(3,)
ispy2 = pyver<(3,)
# __builtins__ is dict
has_long      = 'long'       in __builtins__
has_unicode   = 'unicode'    in __builtins__
has_basestring= 'basestring' in __builtins__
has_bytes     = 'bytes'      in __builtins__
has_buffer    = 'buffer'     in __builtins__

# substitute missing builtins
if has_long:
    long = long
else:
    long = int
if has_basestring:
    basestring = basestring
elif has_bytes:
    basestring = (str, bytes)
else:
    basestring = str
if has_unicode:
    unicode = unicode
else:  # py3 str is unicode
    unicode = str
if has_bytes:
    bytes = bytes
else:  # py2 str is bytes
    bytes = str
if has_buffer:
    buffer = buffer
else:
    buffer = memoryview

# helper variant string
if has_unicode:
    varstr = unicode
else:
    varstr = bytes

# numpy char types
npunicode = 'U'
npbytes = 'S'
if ispy2:
    npstr = npbytes
else:
    npstr = npunicode


def _decode(string):
    try:
        return string.decode('utf-8', 'backslashreplace')
    except:
        return string.decode('CP1252', 'backslashreplace')


def _encode(string):
    return string.encode('utf-8', 'backslashreplace')


def _tostring(string, targ, nptarg, conv, lstres):
    if isinstance(string, targ):  # short cut
        return string
    elif isinstance(string, npscalar):
        return string.astype(nptarg).tostring()
    elif isinstance(string, nparray):
        return string.astype(nptarg).tolist()
    elif isinstance(string, basestring):
        return conv(string)
    elif isinstance(string, (list, tuple)):
        return type(string)(_tostring(s, targ, nptarg, conv, lstres) for s in string)
    else:  # last resort
        return lstres(string)


def tostr(string):
    if ispy2:
        return _tostring(string, bytes, npbytes, _encode, bytes)
    else:
        return _tostring(string, unicode, npunicode, _decode, unicode)


def tobytes(string):
    if ispy2:
        return _tostring(string, bytes, npbytes, _encode, bytes)
    else:
        def _bytes(string):
            return _encode(str(string))
        return _tostring(string, bytes, npbytes, _encode, _bytes)


def tounicode(string):
    if ispy3:
        return _tostring(string, unicode, npbytes, _encode, unicode)
    else:
        def _unicode(string):
            return _decode(str(string))
        return _tostring(string, unicode, npunicode, _decode, _unicode)

# Extract the code attribute of a function. Different implementations
# are for Python 2/3 compatibility.

if ispy2:
    def func_code(f):
        return f.func_code
else:
    def func_code(f):
        return f.__code__
