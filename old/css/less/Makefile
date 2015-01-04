
OUT = main.css

INSTALL_DIR = ../

all: $(OUT)

.PHONY: clean install

clean:
	rm $(OUT)

install: all
	cp $(OUT) $(INSTALL_DIR)

main.css: main.less
	lessc -x $< > $@
