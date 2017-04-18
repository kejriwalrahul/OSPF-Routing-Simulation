all:
	javac -d bin/ Code/simulator.java

run:
	cd bin && java simulator -i 1 -f ../Input/ipfile

rund:
	cd bin && java simulator -i 1 -f ../Input/ipfile -d

rund2:
	cd bin && java simulator -i 2 -f ../Input/ipfile -d

rund3:
	cd bin && java simulator -i 3 -f ../Input/ipfile -d

rund4:
	cd bin && java simulator -i 4 -f ../Input/ipfile -d

rund5:
	cd bin && java simulator -i 0 -f ../Input/ipfile -d

clean:
	rm -f bin/*.class bin/outfile* Code/output*