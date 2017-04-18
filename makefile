all:
	javac -d bin/ Code/simulator.java

run:
	cd bin && java simulator -i 1 -f ../Input/ipfile

rund:
	cd bin && java simulator -i 1 -f ../Input/ipfile -d

rund2:
	cd bin && java simulator -i 3 -f ../Input/ipfile -d

clean:
	rm -f bin/*.class Code/output*