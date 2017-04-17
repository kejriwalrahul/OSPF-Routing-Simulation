all:
	javac -d bin/ Code/simulator.java

run:
	cd bin && java simulator -i 1 -f ../Input/ipfile -d

clean:
	rm -f bin/*.class Code/output*