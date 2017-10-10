Sensor Project: Pedestrian guiding app

The app shows the way to the user's chosen location/address by using different methods.

AR-view: 
The user can use the camera to look around and see te actual direction where the target location is located. The camera displays indicators in augmented reality to guide the user to the right direction.

Compass-view:
The application can also be used similary to a traditional compass, with an arrow that points to the chosen destination.

Map-view:
A map of the surrounding area is also visible to the user on the bottom of the screen. This map shows the users location and the optimal route to the target destination.


Setup instructions:

The app permissions need to be enabled manually from the device's settings

If the camera-view image is zoomed too much by default, change the scale numbers on lines 488-494 of cameraFragment.java to lower ones for example 1.0. (the default values for these are 1.5)

