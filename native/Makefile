#!/usr/bin/make -f 
CC=./agcc.pl

razorclaw: sqlite3.o superuser
	$(CC) $(CFLAGS) -o $@ $@.c $<

superuser:
	make -C $@ all

%.o: %.c
	$(CC) -c -o $@ $<

clean:
	rm -f sqlite3.o razorclaw
	make -C superuser clean

.PHONY: clean superuser
