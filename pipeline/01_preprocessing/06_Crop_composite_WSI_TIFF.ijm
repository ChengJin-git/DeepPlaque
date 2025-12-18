// Use the reference image pixel dimensions to estimate how many grid rectangles (crops) can fit in by width and height of the larger ROI
		
		/* spatial calibration:  
		 *  reference crop's resolution = 3.9954 px/µm (0.2503 µm/px)
		 *  WSI-based image's resolution = 3.665 px/µm (0.2729 µm/px)
		 */
 		// resolution of each image input (px/μm)
		var wsiRes = 6.25;
		var refRes = 3.9953602268;
		
		// Determine the resolution ratios between the WSI and reference images
		// Ratio = Reference / WSI = 3.9954 / 3.665 = 1.0894
		var recalibrationRatio = refRes / wsiRes;
		print("The conversion ratio for recalibration = " + recalibrationRatio);
	
	// Define the target size of rectangular ROI as stamp
	var cropXYstart = "remainXY";
	var exportPatches = true;
	var channelsTOComponent = false;
	var saveComponent = true;
	var saveComposite = true;
	var compositeImg = "Abeta+DAPI";
	var composite_overlay = getTitle();
	var WSIfolder = "/path/to/your/WSI/folder";  // TODO: Set your WSI folder path
	
	dir = File.getParent(WSIfolder);
	
	// re-calibration based on the target image resolution (PhenoImager-acquired)
			run("Set Scale...", "distance=0 known=0 unit=pixel global");
			// run("Set Scale...", "distance=366.5 known=100 unit=um global");
	
	// get pixel dimension (w x h) of the WSI opened in viewer 
			selectImage(composite_overlay);
			var wsi_Width_px = getWidth;
			var wsi_Height_px = getHeight;
			print("Dimension of large image = " + wsi_Width_px + "px * " + wsi_Height_px + "px");
			
			// get pixel dimension (w x h) of the reference crop opened in viewer 

			var refcrop_Width_px = 1860;
			var refcrop_Height_px = 1396;
			print("Dimension of reference crop = " + refcrop_Width_px + "px * " + refcrop_Height_px + "px");
			
			// convert both dimensions to estimate in µm using their respective resolutions
			refcrop_Width_um = refcrop_Width_px/refRes;
			refcrop_Height_um = refcrop_Height_px/refRes;
			print("reference crop's image size = " + refcrop_Width_um + "um * " + refcrop_Height_um + "um");
			wsi_Width_um = wsi_Width_px/wsiRes;
			wsi_Height_um = wsi_Height_px/wsiRes;
			print("large ROI within WSI's image size = " + wsi_Width_um + "um * " + wsi_Height_um + "um");
			
			// spacing between the rectangles
			var spacing = 0; 
			var gridArea_px = refcrop_Width_px * refcrop_Height_px;

			
		selectImage(composite_overlay);
		
		// maximum number of grids that will fit along the height & width of WSI by pixels  
	    var numBoxX = floor(wsi_Width_px/refcrop_Width_px); // number of grids that will fit along the width
	    var numBoxY = floor(wsi_Height_px/refcrop_Height_px); // number of grids that will fit along the height
	    
	    // remainder distance left after the maximum no. of grids fit in 
	    var remainX = (wsi_Width_px - (numBoxX*refcrop_Width_px))/2; 
	    var remainY = (wsi_Height_px - (numBoxY*refcrop_Height_px))/2;
	    
    	var numGrids = numBoxX * numBoxY;
		print("estimated to crop " + numGrids + " stamps from within this image");

		var gridIndex = 1;
		
		// draws rectangles in a grid, centred on the X and Y axes and adds to ROI manager
	    for (i=0; i<numBoxY; i++) { 
	    	for (j=0; j<numBoxX; j++) {
	    		
	    		var width = refcrop_Width_px;
	    		var height = refcrop_Height_px;
	    		
	    		// define dimensions of rectangle (x,y,w,h)
		    		if (cropXYstart == "topleft"){
		    			// start cropping from the left top corner of the image (0,0)
		    			makeRectangle((0 + (j*width)), (0 +(i*height)), width, height); 
		    		}
		    		if (cropXYstart == "remainXY"){
			    		// start cropping from (remainX,remainY) instead of the left top corner 
		    			makeRectangle((remainX + (j*width)), (remainY+(i*height)), width, height); 
		    		}
	    		roiManager("add");
	    		roiManager("Show All with labels");
				roiManager("Select", roiManager("count")-1);
				// rename each patch with double padded counter
				var roiName = "patch_" + IJ.pad(gridIndex, 2);
				roiManager("Rename", roiName);
				roiManager("Set Color", "red");
				roiManager("Set Line Width", 15);
				// (optional) export all patches regardless of Abeta+ segmentation as filter 
				// (abetaPatchOnly == false && exportPatches == true) {
				//    run("Duplicate...", "title=patch"); 
				//    rename(roiName);
				//    saveAs("tiff", dir + "/02_patches/" + markerPanel + "/" + ImageID + "_" + roiName + ".tif");
				//    close();
				}
				gridIndex++;
	    	
	    }
	    
	    // get the total no. of patches stamped within WSI crop image
		var patchN = roiManager("count")-1;

    	// (optional) save all patch ROIs 
	 	// roiManager("Save", dir + "/04_ROIsets/" + "patches_" + ImageID + ".zip"); 
	 	
	 	// export overlay log image of all stamps with labels within composite_overlay
    	roiManager("Show All with labels");
    	run("Labels...", "color=white font=20 show bold");
    	run("Flatten");
    	wait(500);
    	saveAs("tiff", dir + "_40X_StampMap.tif");
    	close();
    	// print("saved a stamp map for all of the rectangular ROI grids.");