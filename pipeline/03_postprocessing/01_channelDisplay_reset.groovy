/* Reset histogram of channel intensities for visual inspection based on true signals 
 * then show display (toggle on) of user-defined channels only.
*/

// Import QuPath's dependencies of relevance
import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.gui.scripting.QPEx
import qupath.lib.scripting.QP
import qupath.lib.gui.commands.Commands
import qupath.lib.images.servers.ImageServerMetadata
import qupath.lib.display.ImageDisplay

///////////////////////////////////////////////////////////////////////////////////////////////////

    // Configure image type as immunofluorescence (IF)
    setImageType('FLUORESCENCE');
    
    // Configure the data source of qpproj file to save
    def qupath = getQuPath()
    def project = qupath.getProject()
    def imageData = getCurrentImageData()
    def entry = getProjectEntry()
    
    // Define current image viewer --> extract the channel array 
    def viewer = getCurrentViewer()
    def display = viewer.getImageDisplay()
    def channels = display.availableChannels()  
    
        // println channels.size()

/*************************************************************************************************/
// User input: Define index of channel(s) to reset and show
// The index indicates the position of the element within the array (starts from 0 onwards), i.e. channel 1 has index 0, channel 4 has index 3 etc. 

    // Do you want to reset histogram display of all channels? Yes, then define true. No, then define false.
    def resetAll_channels = true
    // If No, which channel(s) do you want to reset display range for histogram?
    // define by index within "channels" array
    def channels_to_reset = [4]
    
    // Show (i.e. not hide) channel(s), defined by index within "channels" array
    // manual equivalent = toggle on only the defined channel(s), hide display of other channels
    // channels[i,i,i] to show defined channels by index ; channels to show all ; channels[] to show none
    def channels_to_show = channels[4]

/*************************************************************************************************/
             
    if (resetAll_channels == true) {
        
     for (int i = 0; i < channels.size(); i++) {
            
            display.selectedChannels.setAll(channels[i])

            // Get the max display value and reset it to max allowed display value of such channel
            // Set the maximum permissible range for the image display, opt out the default auto-set saturation on display range
            if (channels[i].getMaxDisplay() != channels[i].getMaxAllowed()) {
                String chName = channels[i]
                double autoMax = channels[i].getMaxDisplay()
                double min = channels[i].getMinAllowed()
                double max = channels[i].getMaxAllowed()
                setChannelDisplayRange(chName, min, max)
                println "reset ${chName} from ${autoMax} to ${max}" 
            }
        }
        
    } else if (resetAll_channels == false){
             
        // reset defined channels' display range for histogram
        channels.eachWithIndex{channelInfo, x->
        
            if(channels_to_reset.contains(x))
                
                viewer.getImageDisplay().setChannelSelected(channelInfo, true)
                
                // Get the max display value and reset it to max allowed display value of such channel
                // Set the maximum permissible range for the image display, opt out the default auto-set saturation on display range
                if (channels[x].getMaxDisplay() != channels[x].getMaxAllowed()) {
                    
                    String chName = channels[x]
                    double autoMax = channels[x].getMaxDisplay()
                    double min = channels[x].getMinAllowed()
                    double max = channels[x].getMaxAllowed()
                    setChannelDisplayRange(chName, min, max)
                    println "reset ${chName} from ${autoMax} to ${max}" 
                    
                 }
                
            else
                viewer.getImageDisplay().setChannelSelected(channelInfo, false)
        }
        
    }
        
        // display only the user-defined channel(s)
        display.selectedChannels.setAll(channels_to_show)
        viewer.repaintEntireImage()
        
    entry.saveImageData(imageData)
    
    // update the temp project file
    qupath.refreshProject()
    
    // save the project.qpproj file 
    project.syncChanges()