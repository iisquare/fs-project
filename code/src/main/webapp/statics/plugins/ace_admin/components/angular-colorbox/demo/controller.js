angular.module('colorboxdemo.controllers', []).
    controller('ColorboxCtrl', function ($scope, $location) {
        $scope.imagesForGallery = [];
        $scope.setApproot = function (appRoot) {
            //only change when needed.
            if ($scope.approot && appRoot === $scope.approot) {
                return;
            }
            $scope.approot = appRoot;
            $scope.imagesForGallery = [
                {
                    thumb: appRoot + 'images/thumb/image1.jpg',
                    small: appRoot + 'images/small/image1.jpg',
                    large: appRoot + 'images/large/image1.jpg'
                },
                {
                    thumb: appRoot + 'images/thumb/image2.jpg',
                    small: appRoot + 'images/small/image2.jpg',
                    large: appRoot + 'images/large/image2.jpg'
                },
                {
                    thumb: appRoot + 'images/thumb/image3.jpg',
                    small: appRoot + 'images/small/image3.jpg',
                    large: appRoot + 'images/large/image3.jpg'
                },
                {
                    thumb: appRoot + 'images/thumb/image4.jpg',
                    small: appRoot + 'images/small/image4.jpg',
                    large: appRoot + 'images/large/image4.jpg'
                },
                {
                    thumb: appRoot + 'images/thumb/image5.jpg',
                    small: appRoot + 'images/small/image5.jpg',
                    large: appRoot + 'images/large/image5.jpg'
                }
            ];

            $scope.zoomModel1 = $scope.imagesForGallery[0];
            $scope.zoomModel2 = $scope.imagesForGallery[1];

            $scope.zoomModelGallery01 = $scope.imagesForGallery[1];
            $scope.zoomModelGallery04 = $scope.imagesForGallery[3];
            $scope.zoomModelGallery05 = $scope.imagesForGallery[0];

            $scope.colorboxWithCallbacks = {
                opacity: 0.5,
                open: false,
                href: appRoot + 'images/large/image1.jpg',
                onOpen: function () {
                    alert('on open');
                },
                onClosed: function () {
                    alert('on closed');
                },
                onLoad: function () {
                    alert('on load');
                },
                onComplete: function () {
                    alert('on complete');
                },
                onCleanup: function () {
                    alert('on cleanup');
                }
            };
        };

        //default
        $scope.setApproot('');

        $scope.zoomOptions = {
            scrollZoom: true,
            zoomWindowWidth: 600,
            zoomWindowHeight: 600,
            easing: true,
            zoomWindowFadeIn: 500,
            zoomWindowFadeOut: 500,
            lensFadeIn: 500,
            lensFadeOut: 500,

            initial: 'small'
        };

        $scope.zoomOptionsGallery01 = {
            scrollZoom: true,
            zoomWindowWidth: 600,
            zoomWindowHeight: 600,
            easing: true,
            zoomWindowFadeIn: 500,
            zoomWindowFadeOut: 500,
            lensFadeIn: 500,
            lensFadeOut: 500,

            initial: 'small',

            gallery: 'gallery_01',
            cursor: 'pointer',
            galleryActiveClass: "active",
            imageCrossfade: true,
            loadingIcon: false
        };

        $scope.zoomOptionsGallery04 = {
            scrollZoom: true,
            zoomWindowWidth: 600,
            zoomWindowHeight: 600,
            easing: true,
            zoomWindowFadeIn: 500,
            zoomWindowFadeOut: 500,
            lensFadeIn: 500,
            lensFadeOut: 500,

            initial: 'small',

            gallery: 'gallery_04',
            cursor: 'pointer',
            galleryActiveClass: "active",
            imageCrossfade: true,
            loadingIcon: false
        };

        $scope.setActiveImageInGallery = function (prop, img) {
            $scope[prop] = img;
            //console.log(img);
        };
        $scope.setScopeValue = function (prop, value) {
            $scope[prop] = value;
        };

    });


