package application.test.cmd;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class CMDArgs {
	
    @Parameter(description =  "extra_args")
    public List<String> parameters = new ArrayList<String>();
 
    @Parameter(names = { "--log", "--verbose" }, description = "Level of verbosity")
    public Integer verbose = 1;
 
    @Parameter(names = "--debug", description = "Debug mode")
    public boolean debug = false;    
    
    @DynamicParameter(names = "--D", description = "Dynamic parameters go here")
    public Map<String, String> dynamicParams = new HashMap<String, String>();    
    
    @Parameter(names = "--root", description = "Project root")
    public String root;
 
    @Parameter(names = "--wdir", description = "Working dir")
    public String wdir;    


    @Parameter(names = "--generate_kernerls", description = "")
    public String[] generate_kernerls = new String[] {
    		"100","20"
    };  
    
    public String[] toArray() throws IllegalArgumentException, IllegalAccessException {
        Class className = CMDArgs.class;
        Field[] fields = className.getDeclaredFields();
        ArrayList<String> args = new ArrayList<String>(fields.length);

        for (int i = 0; i < fields.length; i++) {
        	fields[i].setAccessible(true);
//            System.out.println(fields[i].getName());
            Parameter anno = fields[i].getDeclaredAnnotation(Parameter.class);
            if (anno != null) {            	
            	for (String name : anno.names()) {
//                  System.out.println(Arrays.toString(name));
            		args.add(name);
            		Object value = fields[i].get(this);
            		
            	    if (value!=null && value.getClass().isArray()){
            	    	Object[] objArray = (Object[]) value;
            	    	for (Object object : objArray) {
							args.add(object.toString());
						}
            	    }else if(value!=null && value.getClass() == Iterable.class) {
            	    	Iterable collection = (Iterable) value;            	    	
            	    	for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
            	    		args.add(iterator.next().toString());
            	    	}
            	    }else if (value!=null) {
            	    	args.add(value.toString());
            	    }
				}
            }
            
        }
        
        return args.toArray(new String[args.size()]);
    }
    
}
