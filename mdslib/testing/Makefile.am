
include @top_builddir@/Makefile.inc
include ../../testing/testing.am


AM_CFLAGS = $(TARGET_ARCH) $(WARNFLAGS) $(TEST_CFLAGS) 
AM_CXXFLAGS = $(TARGET_ARCH) $(WARNFLAGS) -Wno-deprecated @CXXFLAGS@ $(TEST_CFLAGS)
AM_LDFLAGS = -L@MAKESHLIBDIR@ $(RPATHLINK),@MAKESHLIBDIR@ 
LDADD = @LIBS@ $(TEST_LIBS) -lMdsLib -lTdiShr -lTreeShr -lMdsShr 

## ////////////////////////////////////////////////////////////////////////// ##
## // TESTS  //////////////////////////////////////////////////////////////// ##
## ////////////////////////////////////////////////////////////////////////// ##

TEST_EXTENSIONS = .py .pl
AM_DEFAULT_SOURCE_EXT = .c

TESTS = \
        dtype_test
        
#        mdslib_ctest

VALGRIND_SUPPRESSIONS_FILES = \
			      $(top_srcdir)/conf/valgrind-mdsplus.supp

#
# Files produced by tests that must be purged
#
MOSTLYCLEANFILES = *.out


## ////////////////////////////////////////////////////////////////////////// ##
## // TARGETS  ////////////////////////////////////////////////////////////// ##
## ////////////////////////////////////////////////////////////////////////// ##

 
print_shell:
	@echo "SHELL = ${SHELL}"


all-local: $(TESTS)
clean-local: clean-local-tests

check_PROGRAMS = $(TESTS)
check_SCRIPTS  = 


