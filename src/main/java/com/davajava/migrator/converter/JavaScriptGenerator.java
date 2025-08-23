package com.davajava.migrator.converter;

import com.davajava.migrator.converter.AndroidToJavaScriptConverter.*;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Generates JavaScript code from Android components.
 * Supports multiple JavaScript frameworks (React, Vue, Angular, Vanilla).
 */
public class JavaScriptGenerator {
    private static final Logger logger = Logger.getLogger(JavaScriptGenerator.class.getName());
    
    public String generateComponent(WebComponent component, AndroidConversionOptions options) {
        switch (options.getFramework()) {
            case REACT:
                return generateReactComponent(component, options);
            case VUE:
                return generateVueComponent(component, options);
            case ANGULAR:
                return generateAngularComponent(component, options);
            case VANILLA:
                return generateVanillaComponent(component, options);
            default:
                return generateVanillaComponent(component, options);
        }
    }
    
    public String generateService(WebService service, AndroidConversionOptions options) {
        switch (service.getType()) {
            case SERVICE_WORKER:
                return generateServiceWorker(service, options);
            case BACKGROUND_WORKER:
                return generateBackgroundWorker(service, options);
            case NETWORK_WORKER:
                return generateNetworkWorker(service, options);
            default:
                return generateGenericService(service, options);
        }
    }
    
    public String generateApp(AndroidToJSConversionResult result, AndroidConversionOptions options) {
        switch (options.getFramework()) {
            case REACT:
                return generateReactApp(result, options);
            case VUE:
                return generateVueApp(result, options);
            case ANGULAR:
                return generateAngularApp(result, options);
            case VANILLA:
                return generateVanillaApp(result, options);
            default:
                return generateVanillaApp(result, options);
        }
    }
    
    public String generateRouter(AndroidToJSConversionResult result, AndroidConversionOptions options) {
        switch (options.getFramework()) {
            case REACT:
                return generateReactRouter(result, options);
            case VUE:
                return generateVueRouter(result, options);
            case ANGULAR:
                return generateAngularRouter(result, options);
            case VANILLA:
                return generateVanillaRouter(result, options);
            default:
                return generateVanillaRouter(result, options);
        }
    }
    
    private String generateReactComponent(WebComponent component, AndroidConversionOptions options) {
        StringBuilder code = new StringBuilder();
        
        code.append("import React, { useState, useEffect } from 'react';\n");
        code.append("import './").append(component.getName()).append(".css';\n\n");
        
        code.append("const ").append(component.getName()).append(" = () => {\n");
        code.append("  // Component state\n");
        code.append("  const [loading, setLoading] = useState(false);\n");
        code.append("  const [data, setData] = useState(null);\n\n");
        
        // Generate state for UI components
        for (UIElement element : component.getUIComponents()) {
            if ("EditText".equals(element.getAndroidType())) {
                String fieldName = element.getId() != null ? 
                    toCamelCase(element.getId()) : "inputValue";
                code.append("  const [").append(fieldName).append(", set")
                    .append(capitalize(fieldName)).append("] = useState('');\n");
            }
        }
        
        code.append("\n  // Component effects\n");
        code.append("  useEffect(() => {\n");
        code.append("    // Component initialization\n");
        code.append("    initializeComponent();\n");
        code.append("  }, []);\n\n");
        
        // Generate API adapter functions
        code.append("  // API adapter functions\n");
        for (APIAdapter adapter : component.getAPIAdapters()) {
            code.append("  const ").append(toCamelCase(adapter.getAndroidAPI()))
                .append("Adapter = {\n");
            code.append("    ").append(adapter.getMethod()).append(": () => {\n");
            code.append("      // ").append(adapter.getAndroidAPI()).append(".")
                .append(adapter.getMethod()).append(" -> ").append(adapter.getWebAPI()).append("\n");
            code.append("      ").append(generateWebAPICall(adapter)).append("\n");
            code.append("    }\n");
            code.append("  };\n\n");
        }
        
        code.append("  // Component methods\n");
        code.append("  const initializeComponent = async () => {\n");
        code.append("    try {\n");
        code.append("      setLoading(true);\n");
        code.append("      // TODO: Initialize component data\n");
        code.append("    } catch (error) {\n");
        code.append("      console.error('Error initializing component:', error);\n");
        code.append("    } finally {\n");
        code.append("      setLoading(false);\n");
        code.append("    }\n");
        code.append("  };\n\n");
        
        // Generate event handlers
        for (UIElement element : component.getUIComponents()) {
            if (element.getEvents().containsKey("onClick")) {
                code.append("  const handle").append(capitalize(element.getId() != null ? 
                    element.getId() : "button")).append("Click = () => {\n");
                code.append("    // Handle ").append(element.getAndroidType()).append(" click\n");
                code.append("    console.log('Button clicked');\n");
                code.append("  };\n\n");
            }
        }
        
        code.append("  // Render component\n");
        code.append("  if (loading) {\n");
        code.append("    return <div className=\"loading\">Loading...</div>;\n");
        code.append("  }\n\n");
        
        code.append("  return (\n");
        code.append("    <div className=\"").append(kebabCase(component.getName())).append("\">\n");
        
        // Generate JSX for UI elements
        for (UIElement element : component.getUIComponents()) {
            code.append("      ").append(generateReactElement(element)).append("\n");
        }
        
        code.append("    </div>\n");
        code.append("  );\n");
        code.append("};\n\n");
        
        code.append("export default ").append(component.getName()).append(";\n");
        
        return code.toString();
    }
    
    private String generateReactElement(UIElement element) {
        StringBuilder jsx = new StringBuilder();
        
        switch (element.getAndroidType()) {
            case "TextView":
                jsx.append("<span className=\"text-view\"");
                if (element.getId() != null) {
                    jsx.append(" id=\"").append(element.getId()).append("\"");
                }
                jsx.append(">");
                jsx.append(element.getProperties().getOrDefault("textContent", "Text"));
                jsx.append("</span>");
                break;
                
            case "Button":
                jsx.append("<button className=\"android-button\"");
                if (element.getId() != null) {
                    jsx.append(" id=\"").append(element.getId()).append("\"");
                }
                if (element.getEvents().containsKey("onClick")) {
                    jsx.append(" onClick={handle").append(capitalize(element.getId() != null ? 
                        element.getId() : "button")).append("Click}");
                }
                jsx.append(">");
                jsx.append(element.getProperties().getOrDefault("textContent", "Button"));
                jsx.append("</button>");
                break;
                
            case "EditText":
                jsx.append("<input className=\"edit-text\"");
                if (element.getId() != null) {
                    jsx.append(" id=\"").append(element.getId()).append("\"");
                }
                jsx.append(" type=\"text\"");
                String placeholder = element.getProperties().get("placeholder");
                if (placeholder != null) {
                    jsx.append(" placeholder=\"").append(placeholder).append("\"");
                }
                String fieldName = element.getId() != null ? 
                    toCamelCase(element.getId()) : "inputValue";
                jsx.append(" value={").append(fieldName).append("}");
                jsx.append(" onChange={(e) => set").append(capitalize(fieldName))
                   .append("(e.target.value)}");
                jsx.append(" />");
                break;
                
            case "ImageView":
                jsx.append("<img className=\"image-view\"");
                if (element.getId() != null) {
                    jsx.append(" id=\"").append(element.getId()).append("\"");
                }
                String src = element.getProperties().get("src");
                if (src != null) {
                    jsx.append(" src=\"").append(src).append("\"");
                }
                String alt = element.getProperties().get("alt");
                if (alt != null) {
                    jsx.append(" alt=\"").append(alt).append("\"");
                }
                jsx.append(" />");
                break;
                
            case "LinearLayout":
                jsx.append("<div className=\"linear-layout");
                if (element.getCssClasses().contains("vertical")) {
                    jsx.append(" vertical");
                } else {
                    jsx.append(" horizontal");
                }
                jsx.append("\"");
                if (element.getId() != null) {
                    jsx.append(" id=\"").append(element.getId()).append("\"");
                }
                jsx.append(">");
                jsx.append("</div>");
                break;
                
            default:
                jsx.append("<div className=\"").append(kebabCase(element.getAndroidType())).append("\"");
                if (element.getId() != null) {
                    jsx.append(" id=\"").append(element.getId()).append("\"");
                }
                jsx.append(">");
                jsx.append("<!-- ").append(element.getAndroidType()).append(" -->");
                jsx.append("</div>");
        }
        
        return jsx.toString();
    }
    
    private String generateVueComponent(WebComponent component, AndroidConversionOptions options) {
        StringBuilder code = new StringBuilder();
        
        code.append("<template>\n");
        code.append("  <div class=\"").append(kebabCase(component.getName())).append("\">\n");
        code.append("    <div v-if=\"loading\" class=\"loading\">Loading...</div>\n");
        code.append("    <div v-else>\n");
        
        // Generate template for UI elements
        for (UIElement element : component.getUIComponents()) {
            code.append("      ").append(generateVueElement(element)).append("\n");
        }
        
        code.append("    </div>\n");
        code.append("  </div>\n");
        code.append("</template>\n\n");
        
        code.append("<script>\n");
        code.append("export default {\n");
        code.append("  name: '").append(component.getName()).append("',\n");
        code.append("  data() {\n");
        code.append("    return {\n");
        code.append("      loading: false,\n");
        code.append("      data: null,\n");
        
        // Generate data properties for UI components
        for (UIElement element : component.getUIComponents()) {
            if ("EditText".equals(element.getAndroidType())) {
                String fieldName = element.getId() != null ? 
                    toCamelCase(element.getId()) : "inputValue";
                code.append("      ").append(fieldName).append(": '',\n");
            }
        }
        
        code.append("    };\n");
        code.append("  },\n");
        
        code.append("  mounted() {\n");
        code.append("    this.initializeComponent();\n");
        code.append("  },\n");
        
        code.append("  methods: {\n");
        code.append("    async initializeComponent() {\n");
        code.append("      try {\n");
        code.append("        this.loading = true;\n");
        code.append("        // TODO: Initialize component data\n");
        code.append("      } catch (error) {\n");
        code.append("        console.error('Error initializing component:', error);\n");
        code.append("      } finally {\n");
        code.append("        this.loading = false;\n");
        code.append("      }\n");
        code.append("    },\n");
        
        // Generate API adapter methods
        for (APIAdapter adapter : component.getAPIAdapters()) {
            code.append("    ").append(toCamelCase(adapter.getAndroidAPI()))
                .append(capitalize(adapter.getMethod())).append("() {\n");
            code.append("      // ").append(adapter.getAndroidAPI()).append(".")
                .append(adapter.getMethod()).append(" -> ").append(adapter.getWebAPI()).append("\n");
            code.append("      ").append(generateWebAPICall(adapter)).append("\n");
            code.append("    },\n");
        }
        
        // Generate event handlers
        for (UIElement element : component.getUIComponents()) {
            if (element.getEvents().containsKey("onClick")) {
                code.append("    handle").append(capitalize(element.getId() != null ? 
                    element.getId() : "button")).append("Click() {\n");
                code.append("      // Handle ").append(element.getAndroidType()).append(" click\n");
                code.append("      console.log('Button clicked');\n");
                code.append("    },\n");
            }
        }
        
        code.append("  }\n");
        code.append("};\n");
        code.append("</script>\n\n");
        
        code.append("<style scoped>\n");
        code.append("/* Component styles */\n");
        code.append("</style>\n");
        
        return code.toString();
    }
    
    private String generateVueElement(UIElement element) {
        StringBuilder template = new StringBuilder();
        
        switch (element.getAndroidType()) {
            case "TextView":
                template.append("<span class=\"text-view\"");
                if (element.getId() != null) {
                    template.append(" id=\"").append(element.getId()).append("\"");
                }
                template.append(">");
                template.append(element.getProperties().getOrDefault("textContent", "Text"));
                template.append("</span>");
                break;
                
            case "Button":
                template.append("<button class=\"android-button\"");
                if (element.getId() != null) {
                    template.append(" id=\"").append(element.getId()).append("\"");
                }
                if (element.getEvents().containsKey("onClick")) {
                    template.append(" @click=\"handle").append(capitalize(element.getId() != null ? 
                        element.getId() : "button")).append("Click\"");
                }
                template.append(">");
                template.append(element.getProperties().getOrDefault("textContent", "Button"));
                template.append("</button>");
                break;
                
            case "EditText":
                template.append("<input class=\"edit-text\"");
                if (element.getId() != null) {
                    template.append(" id=\"").append(element.getId()).append("\"");
                }
                template.append(" type=\"text\"");
                String placeholder = element.getProperties().get("placeholder");
                if (placeholder != null) {
                    template.append(" placeholder=\"").append(placeholder).append("\"");
                }
                String fieldName = element.getId() != null ? 
                    toCamelCase(element.getId()) : "inputValue";
                template.append(" v-model=\"").append(fieldName).append("\"");
                template.append(" />");
                break;
                
            default:
                template.append("<div class=\"").append(kebabCase(element.getAndroidType())).append("\"");
                if (element.getId() != null) {
                    template.append(" id=\"").append(element.getId()).append("\"");
                }
                template.append(">");
                template.append("<!-- ").append(element.getAndroidType()).append(" -->");
                template.append("</div>");
        }
        
        return template.toString();
    }
    
    private String generateVanillaComponent(WebComponent component, AndroidConversionOptions options) {
        StringBuilder code = new StringBuilder();
        
        code.append("class ").append(component.getName()).append(" {\n");
        code.append("  constructor(container) {\n");
        code.append("    this.container = container;\n");
        code.append("    this.loading = false;\n");
        code.append("    this.data = null;\n");
        code.append("    this.init();\n");
        code.append("  }\n\n");
        
        code.append("  async init() {\n");
        code.append("    try {\n");
        code.append("      this.setLoading(true);\n");
        code.append("      await this.initializeComponent();\n");
        code.append("      this.render();\n");
        code.append("      this.bindEvents();\n");
        code.append("    } catch (error) {\n");
        code.append("      console.error('Error initializing component:', error);\n");
        code.append("    } finally {\n");
        code.append("      this.setLoading(false);\n");
        code.append("    }\n");
        code.append("  }\n\n");
        
        code.append("  async initializeComponent() {\n");
        code.append("    // TODO: Initialize component data\n");
        code.append("  }\n\n");
        
        // Generate API adapter methods
        for (APIAdapter adapter : component.getAPIAdapters()) {
            code.append("  ").append(toCamelCase(adapter.getAndroidAPI()))
                .append(capitalize(adapter.getMethod())).append("() {\n");
            code.append("    // ").append(adapter.getAndroidAPI()).append(".")
                .append(adapter.getMethod()).append(" -> ").append(adapter.getWebAPI()).append("\n");
            code.append("    ").append(generateWebAPICall(adapter)).append("\n");
            code.append("  }\n\n");
        }
        
        code.append("  render() {\n");
        code.append("    if (this.loading) {\n");
        code.append("      this.container.innerHTML = '<div class=\"loading\">Loading...</div>';\n");
        code.append("      return;\n");
        code.append("    }\n\n");
        
        code.append("    this.container.innerHTML = `\n");
        code.append("      <div class=\"").append(kebabCase(component.getName())).append("\">\n");
        
        // Generate HTML for UI elements
        for (UIElement element : component.getUIComponents()) {
            code.append("        ").append(generateVanillaElement(element)).append("\n");
        }
        
        code.append("      </div>\n");
        code.append("    `;\n");
        code.append("  }\n\n");
        
        code.append("  bindEvents() {\n");
        
        // Generate event binding
        for (UIElement element : component.getUIComponents()) {
            if (element.getEvents().containsKey("onClick")) {
                code.append("    const ").append(element.getId() != null ? 
                    element.getId() : "button").append(" = this.container.querySelector('#")
                    .append(element.getId() != null ? element.getId() : "button").append("');\n");
                code.append("    ").append(element.getId() != null ? 
                    element.getId() : "button").append(".addEventListener('click', () => {\n");
                code.append("      this.handle").append(capitalize(element.getId() != null ? 
                    element.getId() : "button")).append("Click();\n");
                code.append("    });\n\n");
            }
        }
        
        code.append("  }\n\n");
        
        // Generate event handlers
        for (UIElement element : component.getUIComponents()) {
            if (element.getEvents().containsKey("onClick")) {
                code.append("  handle").append(capitalize(element.getId() != null ? 
                    element.getId() : "button")).append("Click() {\n");
                code.append("    // Handle ").append(element.getAndroidType()).append(" click\n");
                code.append("    console.log('Button clicked');\n");
                code.append("  }\n\n");
            }
        }
        
        code.append("  setLoading(loading) {\n");
        code.append("    this.loading = loading;\n");
        code.append("    this.render();\n");
        code.append("  }\n");
        
        code.append("}\n\n");
        code.append("export default ").append(component.getName()).append(";\n");
        
        return code.toString();
    }
    
    private String generateVanillaElement(UIElement element) {
        StringBuilder html = new StringBuilder();
        
        switch (element.getAndroidType()) {
            case "TextView":
                html.append("<span class=\"text-view\"");
                if (element.getId() != null) {
                    html.append(" id=\"").append(element.getId()).append("\"");
                }
                html.append(">");
                html.append(element.getProperties().getOrDefault("textContent", "Text"));
                html.append("</span>");
                break;
                
            case "Button":
                html.append("<button class=\"android-button\"");
                if (element.getId() != null) {
                    html.append(" id=\"").append(element.getId()).append("\"");
                }
                html.append(">");
                html.append(element.getProperties().getOrDefault("textContent", "Button"));
                html.append("</button>");
                break;
                
            case "EditText":
                html.append("<input class=\"edit-text\"");
                if (element.getId() != null) {
                    html.append(" id=\"").append(element.getId()).append("\"");
                }
                html.append(" type=\"text\"");
                String placeholder = element.getProperties().get("placeholder");
                if (placeholder != null) {
                    html.append(" placeholder=\"").append(placeholder).append("\"");
                }
                html.append(" />");
                break;
                
            default:
                html.append("<div class=\"").append(kebabCase(element.getAndroidType())).append("\"");
                if (element.getId() != null) {
                    html.append(" id=\"").append(element.getId()).append("\"");
                }
                html.append(">");
                html.append("<!-- ").append(element.getAndroidType()).append(" -->");
                html.append("</div>");
        }
        
        return html.toString();
    }
    
    // Additional methods for Angular, service workers, apps, and routers would continue here...
    
    private String generateAngularComponent(WebComponent component, AndroidConversionOptions options) {
        return "// Angular component generation not implemented yet";
    }
    
    private String generateServiceWorker(WebService service, AndroidConversionOptions options) {
        return "// Service Worker generation\n" +
               "self.addEventListener('install', (event) => {\n" +
               "  console.log('Service Worker installing');\n" +
               "});\n\n" +
               "self.addEventListener('activate', (event) => {\n" +
               "  console.log('Service Worker activating');\n" +
               "});\n\n" +
               "self.addEventListener('fetch', (event) => {\n" +
               "  // Handle fetch events\n" +
               "});";
    }
    
    private String generateBackgroundWorker(WebService service, AndroidConversionOptions options) {
        return "// Background Worker\n" +
               "self.onmessage = function(e) {\n" +
               "  const { type, data } = e.data;\n" +
               "  \n" +
               "  switch (type) {\n" +
               "    case 'START_BACKGROUND_TASK':\n" +
               "      // Handle background task\n" +
               "      break;\n" +
               "    default:\n" +
               "      console.log('Unknown message type:', type);\n" +
               "  }\n" +
               "};";
    }
    
    private String generateNetworkWorker(WebService service, AndroidConversionOptions options) {
        return "// Network Worker\n" +
               "class NetworkWorker {\n" +
               "  constructor() {\n" +
               "    this.cache = new Map();\n" +
               "  }\n" +
               "  \n" +
               "  async fetchWithCache(url, options = {}) {\n" +
               "    const cacheKey = url + JSON.stringify(options);\n" +
               "    \n" +
               "    if (this.cache.has(cacheKey)) {\n" +
               "      return this.cache.get(cacheKey);\n" +
               "    }\n" +
               "    \n" +
               "    const response = await fetch(url, options);\n" +
               "    const data = await response.json();\n" +
               "    \n" +
               "    this.cache.set(cacheKey, data);\n" +
               "    return data;\n" +
               "  }\n" +
               "}\n" +
               "\n" +
               "export default new NetworkWorker();";
    }
    
    private String generateGenericService(WebService service, AndroidConversionOptions options) {
        return "// Generic Service: " + service.getName();
    }
    
    private String generateReactApp(AndroidToJSConversionResult result, AndroidConversionOptions options) {
        StringBuilder code = new StringBuilder();
        
        code.append("import React from 'react';\n");
        code.append("import ReactDOM from 'react-dom';\n");
        code.append("import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';\n");
        
        // Import components
        for (WebComponent component : result.getWebComponents()) {
            code.append("import ").append(component.getName())
                .append(" from './").append(component.getName()).append("';\n");
        }
        
        code.append("\nconst App = () => {\n");
        code.append("  return (\n");
        code.append("    <Router>\n");
        code.append("      <div className=\"app\">\n");
        code.append("        <Routes>\n");
        
        // Generate routes
        for (WebComponent component : result.getWebComponents()) {
            String path = component.getRoute() != null ? component.getRoute() : "/" + component.getName().toLowerCase();
            code.append("          <Route path=\"").append(path).append("\" element={<")
                .append(component.getName()).append(" />} />\n");
        }
        
        code.append("        </Routes>\n");
        code.append("      </div>\n");
        code.append("    </Router>\n");
        code.append("  );\n");
        code.append("};\n\n");
        
        code.append("ReactDOM.render(<App />, document.getElementById('app'));\n");
        
        return code.toString();
    }
    
    private String generateVueApp(AndroidToJSConversionResult result, AndroidConversionOptions options) {
        return "// Vue app generation not implemented yet";
    }
    
    private String generateAngularApp(AndroidToJSConversionResult result, AndroidConversionOptions options) {
        return "// Angular app generation not implemented yet";
    }
    
    private String generateVanillaApp(AndroidToJSConversionResult result, AndroidConversionOptions options) {
        StringBuilder code = new StringBuilder();
        
        // Import components
        for (WebComponent component : result.getWebComponents()) {
            code.append("import ").append(component.getName())
                .append(" from './").append(component.getName()).append(".js';\n");
        }
        
        code.append("\nclass App {\n");
        code.append("  constructor() {\n");
        code.append("    this.currentComponent = null;\n");
        code.append("    this.container = document.getElementById('app');\n");
        code.append("    this.init();\n");
        code.append("  }\n\n");
        
        code.append("  init() {\n");
        code.append("    this.setupRouting();\n");
        code.append("    this.navigate(window.location.pathname);\n");
        code.append("  }\n\n");
        
        code.append("  setupRouting() {\n");
        code.append("    window.addEventListener('popstate', (e) => {\n");
        code.append("      this.navigate(window.location.pathname);\n");
        code.append("    });\n");
        code.append("  }\n\n");
        
        code.append("  navigate(path) {\n");
        code.append("    if (this.currentComponent) {\n");
        code.append("      this.currentComponent = null;\n");
        code.append("    }\n\n");
        
        code.append("    switch (path) {\n");
        
        // Generate routing cases
        for (WebComponent component : result.getWebComponents()) {
            String path = component.getRoute() != null ? component.getRoute() : "/" + component.getName().toLowerCase();
            code.append("      case '").append(path).append("':\n");
            code.append("        this.currentComponent = new ").append(component.getName())
                .append("(this.container);\n");
            code.append("        break;\n");
        }
        
        code.append("      default:\n");
        if (!result.getWebComponents().isEmpty()) {
            WebComponent mainComponent = result.getWebComponents().stream()
                .filter(WebComponent::isMainComponent)
                .findFirst()
                .orElse(result.getWebComponents().get(0));
            code.append("        this.currentComponent = new ").append(mainComponent.getName())
                .append("(this.container);\n");
        }
        code.append("    }\n");
        code.append("  }\n");
        code.append("}\n\n");
        
        code.append("new App();\n");
        
        return code.toString();
    }
    
    private String generateReactRouter(AndroidToJSConversionResult result, AndroidConversionOptions options) {
        return "// React Router is integrated in the main App component";
    }
    
    private String generateVueRouter(AndroidToJSConversionResult result, AndroidConversionOptions options) {
        return "// Vue Router generation not implemented yet";
    }
    
    private String generateAngularRouter(AndroidToJSConversionResult result, AndroidConversionOptions options) {
        return "// Angular Router generation not implemented yet";
    }
    
    private String generateVanillaRouter(AndroidToJSConversionResult result, AndroidConversionOptions options) {
        return "// Vanilla routing is integrated in the main App class";
    }
    
    private String generateWebAPICall(APIAdapter adapter) {
        switch (adapter.getWebAPI()) {
            case "localStorage":
                return "localStorage." + adapter.getWebMethod() + "();";
            case "fetch":
                return "await fetch(url, options);";
            case "HTMLAudioElement":
                return "audioElement." + adapter.getWebMethod() + ";";
            default:
                return "// " + adapter.getWebAPI() + "." + adapter.getWebMethod();
        }
    }
    
    // Utility methods
    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] parts = str.split("_");
        StringBuilder result = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            result.append(capitalize(parts[i]));
        }
        return result.toString();
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    private String kebabCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}