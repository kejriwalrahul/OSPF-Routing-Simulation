all:
	javac -d bin/ Code/simulator.java

run:
	cd bin && java simulator -i $(i) -f ../Input/ipfile -d

# ----------------------------------------
# Run sample nodes with default parameters

run_n1:
	cd bin && java simulator -i 1 -f ../Input/ipfile -d

run_n2:
	cd bin && java simulator -i 2 -f ../Input/ipfile -d

run_n3:
	cd bin && java simulator -i 3 -f ../Input/ipfile -d

run_n4:
	cd bin && java simulator -i 4 -f ../Input/ipfile -d

run_n0:
	cd bin && java simulator -i 0 -f ../Input/ipfile -d

clean:
	rm -f bin/*.class bin/outfile* Code/output*