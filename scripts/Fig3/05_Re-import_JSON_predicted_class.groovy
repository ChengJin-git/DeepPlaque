/* reimport JSON file to load back contour of DL-predicted objects, to assign predicted class after thresholding model confidence
 * Import from an array list of GeoJSON objects as key-value pairing texts in .json 
 * a QuPath object is a GeoJSON Feature ; a ROI is a Geometry
 * coordinates in pixel units by default, with (0,0) the top left of the image
 * 
 * import an array of Polygon coordinates to re-create those polygons 
 * --> Assigning predicted class back to original objects
*/
 
import qupath.lib.io.GsonTools
import static qupath.lib.gui.scripting.QPEx.*

    boolean prettyPrint = true
    def gson = GsonTools.getInstance(prettyPrint)
    
    // define type of geoJSON feature as list of objects (feature collection) or single object (feature) 
    def type = new com.google.gson.reflect.TypeToken<List<qupath.lib.objects.PathObject>>() {}.getType();
    
    def imageData = getCurrentImageData()
    def server = getCurrentServer()
        
        // extract identifier to match with GeoJSON objects to be imported
        def imageID = getProjectEntry().getMetadataValue("ID")
        def sampleID = getProjectEntry().getMetadataValue("ID-sample")
            def sample = sampleID.substring(imageID.length()+1,sampleID.length())

        // deserialization from JSON ( fails if your object has a name and not a color, i.e. the color is null)
        def json_fp = new File("/path/to/output/geojsons/100_Scan_" + sample + "_composite_image.json")  // TODO: Set your geojson folder path
        def json = json_fp.text
            
        //deserializedAnnotations = gson.fromJson(json_fp.getText('UTF-8'), type);
        deserializedAnnotations = gson.fromJson(json, type);
        
            deserializedAnnotations.eachWithIndex {
                annotation, i ->
                annotation.setName('DLpredicted_' + (i+1))
                
            }
    
    // Add to your currently-opened image
    addObjects(deserializedAnnotations)
    resolveHierarchy() 
    
        // automatically save the processed ImageData within batch run
        def qupath = getQuPath()
        def project = qupath.getProject()
        def entry = getProjectEntry()

            entry.saveImageData(imageData)
            qupath.refreshProject()
            project.syncChanges()