debug:
	ant debug

release:
	ant release

installdebug: debug
	adb $(OPT) install -r bin/TinyTimeTracker-debug.apk

install: release
	adb $(OPT) install -r bin/TinyTimeTracker-release.apk

uninstall:
	adb uninstall com.firebirdberlin.tinytimetracker

clean:
	ant clean
