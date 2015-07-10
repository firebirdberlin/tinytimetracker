debug:
	ant debug

installdebug: debug
	adb -d install -r bin/TinyTimeTracker-debug.apk

clean:
	ant clean
