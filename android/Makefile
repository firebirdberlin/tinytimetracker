PACKAGE_NAME=com.firebirdberlin.tinytimetracker

debug:
	ant debug

release:
	ant release

installdebug: debug
	adb $(OPT) install -r bin/TinyTimeTracker-debug.apk

install: release
	adb $(OPT) install -r bin/TinyTimeTracker-release.apk

uninstall:
	adb uninstall ${PACKAGE_NAME}

clean:
	ant clean

start:
	adb $(OPT) shell am start -n ${PACKAGE_NAME}/${PACKAGE_NAME}.TinyTimeTracker

on:
	adb shell input keyevent KEYCODE_POWER

clear-data:
	adb shell pm clear ${PACKAGE_NAME}
