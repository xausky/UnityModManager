OBJECTS=$(patsubst %.c,%.o,$(wildcard src/*.c))
CFLAGS=-I include -g
umm:$(OBJECTS) main.c
	gcc $(CFLAGS) -o $@ $^

%.o:%.c
	gcc -c $(CFLAGS) -o $@ $^

clean:
	-rm src/*.o
	-rm umm
	-rm umm.exe
	-rm -rf out
	-rm out.map.txt
	-rm test.pck
