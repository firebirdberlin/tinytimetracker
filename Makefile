debug:
	ant debug

release:
	ant release

installdebug: debug
	adb -d install -r bin/TinyTimeTracker-debug.apk

install: release
	adb -d install -r bin/TinyTimeTracker-release.apk

uninstall:
	adb uninstall com.firebirdberlin.tinytimetracker

clean:
	ant clean
