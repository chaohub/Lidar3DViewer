# Lidar3DViewer

This is Android sample code to demonstrate OpenGL 3D viewer for Microvision Lidar data.

The app currenly only verified on modified Firefly rx3399 box. The Lidar will enumerate as a UVC camera with YUY2 format. The Firefly platform camera HAL is modified to support YUY2 format, and allow the YUY2 data been passed through the platform without format convertion. The Firefly platform mod will be published in different repo.
