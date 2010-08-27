#!/usr/bin/env perl -w
while (<STDIN>) {
    if (m,#\s*define\s+([A-Z0-9_]+)\s+(.*?)\s*(/\*\s*(.*?)\s*\*/)?$,) {
	s//public static final int $1 = $2;/;
	if ($4) {
            $doc = $4 . '.';
	    s,^,/** $doc */\n,;
	}
    }
    if (m/\(void\)\s*fputc\((.*),\s*([a-z]+)\)*/) {
	$file = $2;
	if ($2 eq "stderr") {
	    $file = "System.err";
	} elsif ($2 eq "stdout") {
	    $file = "System.out";
	}
	s//$file.write($1)/;
    }
    if (m/\(void\)\s*fprintf\(([a-z]+),\s*/) {
	$file = $1;
	if ($1 eq "stderr") {
	    $file = "System.err";
	} elsif ($1 eq "stdout") {
	    $file = "System.out";
	}
	s//$file.printf(/;
    }
    if (m/\(void\)\s*printf\(/) {
	s//System.out.printf(/;
    }
    if (m/\(void\)\s*putc\(*/) {
	s//System.out.write(/;
    }

    s/\bsize_t\b/int/g;
    s/\bNULL\b/null/g;
    s/->/./g;
    s/\b_DIAGASSERT\b/assert/g;

    print;
}
