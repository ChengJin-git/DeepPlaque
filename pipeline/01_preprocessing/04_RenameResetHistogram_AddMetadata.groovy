/* reset histogram of Abeta & DAPI channel intensities for visual inspection based on true signals and add metadata for the images
 * 
 * 
 */

// Import QuPath's dependencies of relevance
import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.gui.scripting.QPEx
import qupath.lib.scripting.QP
import qupath.lib.gui.commands.Commands
import qupath.lib.images.ImageData.ImageType
import qupath.lib.images.servers.ImageChannel
import qupath.lib.images.servers.ImageServerMetadata
import qupath.lib.display.*
import javafx.application.Platform
import javax.swing.*
import qupath.imagej.tools.IJTools

//guiscript=true

    // configure image type as immunofluorescence (IF), with calibration of px/um, as prerequisites
       setImageType('FLUORESCENCE');
       setPixelSizeMicrons(0.2503, 0.2503)
    
    // Configure the data source of qpproj file to save
    def qupath = getQuPath()
    def project = qupath.getProject()
    def entry = getProjectEntry()
    def imageName = entry.getImageName()
    
    // Define current image viewer --> overwrite with currentImageData 
    def imageData = getBatchImageData()
    def server = imageData.getServer()
    def viewer = getCurrentViewer()
    def options = viewer.getOverlayOptions()
    
        options.setShowNames(false)
        options.setShowAnnotations(false)
        
//    def imp = getCurrentImageData()
//    def type = imp.getImageType()
//    println imp.methods
//    
//        if(type != null) {
//            
//            print("Image type: " + type)
//    
//        javafx.application.Platform.runLater {
//            Thread.sleep(1000)
//            viewer.setImageData(imageData)
//        }
//    } else {
//           println "null img type" 
//            def meta = getCurrentServer().getMetadata()
//            type = meta.getPixelType()
//            println type 
//            setImageType(FLUORESCENCE)
//       }
//    viewer.repaintEntireImage()
    
    def display = viewer.getImageDisplay()
    def metadata = getCurrentServer().getMetadata()
    def channels_metadata = metadata.getChannels()
    // extract the channel array 
    def channels = display.availableChannels() 
    // println "This image has ${channels.size()} channels "
        def channelNames = channels.collect { c -> c.name }

/*************************************************************************************************/
// User input: Define index of channel(s) to reset and show
// The index indicates the position of the element within the array (starts from 0 onwards), i.e. channel 1 has index 0, channel 4 has index 3 etc. 

//    var dataset = "Iba1+Abeta+DAPI"
//    var dataset = "ThS+Abeta+DAPI"
    var dataset = "Iba1+Abeta+DAPI"

    // (optional) if true, rename channels (auto-generated channel names = "c:1/3 - ID_segmentN_cropN")
    if (dataset == "Iba1+Abeta+DAPI") {setChannelNames("Abeta", "Iba-1", "DAPI")}
    if (dataset == "ThS+Abeta+nucleus") {setChannelNames("Abeta", "ThioS", "nucleus")}

    // Do you want to reset histogram display of all channels? Yes, then define true. No, then define false.
    def resetAll_channels = false
    // If No, which channel(s) do you want to reset display range for histogram?
    // define by index within "channels" array
    def abetaChindex = 0;
        def reset_MinMax = [3]
        def auto_scale = [abetaChindex,1,2]
    
    // User can change the percentage of auto-scale adjustment of the histogram: 
    // Edit > Preferences... > Viewer > Auto Brightness/Contrast saturation % (e.g. from 0.1 to 0.05)
    // Controls percentage of saturated pixels to apply when automatically setting brightness/contrast.
    // (A value of 1 indicates that approximately 1% dark pixels and 1% bright pixels should be saturated.)
    double saturation_percentage = 0.00005
    
    // Show (i.e. not hide) channel(s), defined by index within "channels" array
    // manual equivalent = toggle on only the defined channel(s), hide display of other channels
    // channels[i,i,i] to show defined channels by index ; channels to show all ; channels[] to show none
    def channels_to_show = channels[abetaChindex]
    viewer.repaintEntireImage()
    
    // Define Abeta channel by index in channel list
    def AbetaChannelIndex = channels[abetaChindex]
        // reset Abeta channel in grayscale, also (optional) ThS as magenta
        AbetaChannelIndex.setLUTColor(255, 255, 255)
    
    // (optional) change ThS+ channel's LUT 
    if (dataset == "ThS+Abeta+nucleus") { 
        def ThSchIndex = 2; 
        def ThSchannelIndex = channels[ThSchIndex]
        ThSchannelIndex.setLUTColor(255, 0, 255)
    }

/*************************************************************************************************/

        // reset channel intensity histogram of channel(s)
        if (resetAll_channels == true) {
            
            println "reset all channels"
            
            for (int i = 0; i < channels.size(); i++) {
                    
                    display.selectedChannels.setAll(channels[i])
        
                    // Get the max display value and reset it to max allowed display value of such channel
                    // Set the maximum permissible range for the image display, opt out the default auto-set saturation on display range
                    if (channels[i].getMaxDisplay() != channels[i].getMaxAllowed()) {
                        def chName = channels[i].getName().toString()
                        double currentMax = channels[i].getMaxDisplay()
                        double min = channels[i].getMinAllowed()
                        double max = channels[i].getMaxAllowed()
                        
                        // auto-scale Abeta channel intensity histogram in viewer
                        if (auto_scale == true) {
                            viewer.getImageDisplay().autoSetDisplayRange(channels[i], saturation_percentage)
                            double AutoMin = channels[i].getMinDisplay()
                            double AutoMax = channels[i].getMaxDisplay()
                            println "reset ${chName} from ${currentMax} to range ${AutoMin} - ${AutoMax}"
                        } else {
                            setChannelDisplayRange(channels[i], min, max)
                            println "reset ${chName} 's max from ${currentMax} to ${max}"
                        }
                    }
                }
            viewer.repaintEntireImage()
            
        } else if (resetAll_channels == false) {
           
           println "reset only channels: ${reset_MinMax} , auto-tune: ${auto_scale} "
                 
            // reset defined channels' display range for histogram
            channels.eachWithIndex{channelInfo, x ->
    
                    viewer.getImageDisplay().setChannelSelected(channelInfo, true)
                    
                    // Get the max display value and reset it to max allowed display value of such channel
                    // Set the maximum permissible range for the image display, opt out the default auto-set saturation on display range
                    def chName = channels[x].getName().toString()
                    double currentMax = channels[x].getMaxDisplay()
                    double min = channels[x].getMinAllowed()
                    double max = channels[x].getMaxAllowed()
                    
                    // auto-scale Abeta channel intensity histogram in viewer
                    if (auto_scale.contains(x)) {
                        viewer.getImageDisplay().autoSetDisplayRange(channels[x], saturation_percentage)
                        double AutoMin = channels[x].getMinDisplay()
                        double AutoMax = channels[x].getMaxDisplay()
                        println "reset ${chName} from ${currentMax} to range ${AutoMin} - ${AutoMax}"
                    } else if (reset_MinMax.contains(x)){
                        setChannelDisplayRange(chName, min, max)
                        println "reset ${chName} 's max from ${currentMax} to ${max}"
                    }
            }
                viewer.repaintEntireImage()
        }  
    
    // save the updated channel properties
    // viewer.getImageDisplay().saveChannelColorProperties()
    // show display (toggle on) of user-defined channel(s) only
    display.selectedChannels.setAll(channels_to_show)
    viewer.repaintEntireImage()

/////////////////////////////////////////////////////////////////////////////////////////////////////   

// Add metadata for each image within list (ID, ID-segment, NumPlaques, NumCored, NumFibrillar, NumDiffuse, NumUnclassified, exclude)

   // extract ID, ID-segment, cropID from filename         
    def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
        def idIndex = name.indexOf("_")
        def ID = name.substring(0,idIndex)
        def cropIndex = name.indexOf("_patch")
        def cropID = name.substring(0,cropIndex)
        def patchNum = name.substring(cropIndex+7, name.length())
        def cropIDpatch = cropID + "-" + patchNum
        println cropID
        println patchNum
        
        entry.putMetadataValue("ID-sample", cropID)
        entry.putMetadataValue("cropID-patchN", cropIDpatch)
        
/////////////////////////////////////////////////////////////////////////////////////////////////////      
        
    entry.saveImageData(imageData)
    
    // update the temp project file
    qupath.refreshProject()
    
    // save the project.qpproj file 
    project.syncChanges()
