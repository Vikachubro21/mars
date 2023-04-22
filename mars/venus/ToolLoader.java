package mars.venus;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JMenu;
import mars.tools.MarsTool;
import mars.util.FilenameFinder;

public class ToolLoader {
   private static final String CLASS_PREFIX = "mars.tools.";
   private static final String TOOLS_DIRECTORY_PATH = "mars/tools";
   private static final String TOOLS_MENU_NAME = "Tools";
   private static final String MARSTOOL_INTERFACE = "MarsTool.class";
   private static final String CLASS_EXTENSION = "class";

   public JMenu buildToolsMenu() {
      JMenu menu = null;
      ArrayList marsToolList = this.loadMarsTools();
      if (!marsToolList.isEmpty()) {
         menu = new JMenu("Tools");
         menu.setMnemonic(84);

         for(int i = 0; i < marsToolList.size(); ++i) {
            MarsToolClassAndInstance listItem = (MarsToolClassAndInstance)marsToolList.get(i);
            menu.add(new ToolAction(listItem.marsToolClass, listItem.marsToolInstance.getName()));
         }
      }

      return menu;
   }

   private ArrayList loadMarsTools() {
      ArrayList toolList = new ArrayList();
      ArrayList candidates = FilenameFinder.getFilenameList(this.getClass().getClassLoader(), "mars/tools", "class");
      HashMap tools = new HashMap();

      for(int i = 0; i < candidates.size(); ++i) {
         String file = (String)candidates.get(i);
         if (!tools.containsKey(file)) {
            tools.put(file, file);
            if (!file.equals("MarsTool.class")) {
               try {
                  String toolClassName = "mars.tools." + file.substring(0, file.indexOf("class") - 1);
                  Class clas = Class.forName(toolClassName);
                  if (MarsTool.class.isAssignableFrom(clas) && !Modifier.isAbstract(clas.getModifiers()) && !Modifier.isInterface(clas.getModifiers())) {
                     toolList.add(new MarsToolClassAndInstance(clas, (MarsTool)clas.newInstance()));
                  }
               } catch (Exception var8) {
                  System.out.println("Error instantiating MarsTool from file " + file + ": " + var8);
               }
            }
         }
      }

      return toolList;
   }

   private class MarsToolClassAndInstance {
      Class marsToolClass;
      MarsTool marsToolInstance;

      MarsToolClassAndInstance(Class marsToolClass, MarsTool marsToolInstance) {
         this.marsToolClass = marsToolClass;
         this.marsToolInstance = marsToolInstance;
      }
   }
}
