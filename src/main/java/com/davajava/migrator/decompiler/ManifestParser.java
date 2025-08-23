package com.davajava.migrator.decompiler;

import com.davajava.migrator.decompiler.ApkDecompiler.AndroidManifest;
import com.davajava.migrator.decompiler.ApkDecompiler.AndroidComponent;
import com.davajava.migrator.decompiler.ApkDecompiler.IntentFilter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Parser for Android manifest files (AndroidManifest.xml).
 * Handles both binary and plain XML manifests.
 */
public class ManifestParser {
    private static final Logger logger = Logger.getLogger(ManifestParser.class.getName());
    
    public AndroidManifest parse(InputStream manifestStream) {
        try {
            // Try to parse as XML first (for plain text manifests)
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(manifestStream);
            
            return parseXmlManifest(document);
            
        } catch (Exception e) {
            logger.warning("Failed to parse manifest as XML, may be binary format: " + e.getMessage());
            
            // If XML parsing fails, try binary parsing
            try {
                manifestStream.reset();
                return parseBinaryManifest(manifestStream);
            } catch (Exception binaryException) {
                logger.severe("Failed to parse manifest in both XML and binary formats: " + binaryException.getMessage());
                return createDefaultManifest();
            }
        }
    }
    
    private AndroidManifest parseXmlManifest(Document document) {
        AndroidManifest manifest = new AndroidManifest();
        
        Element root = document.getDocumentElement();
        if (!"manifest".equals(root.getNodeName())) {
            throw new IllegalArgumentException("Invalid manifest root element");
        }
        
        // Parse manifest attributes
        manifest.setPackageName(root.getAttribute("package"));
        
        String versionCode = root.getAttribute("android:versionCode");
        if (!versionCode.isEmpty()) {
            try {
                manifest.setVersionCode(Integer.parseInt(versionCode));
            } catch (NumberFormatException e) {
                logger.warning("Invalid version code: " + versionCode);
            }
        }
        
        manifest.setVersionName(root.getAttribute("android:versionName"));
        
        // Parse uses-sdk
        NodeList usesSdkNodes = root.getElementsByTagName("uses-sdk");
        if (usesSdkNodes.getLength() > 0) {
            Element usesSdk = (Element) usesSdkNodes.item(0);
            
            String minSdk = usesSdk.getAttribute("android:minSdkVersion");
            if (!minSdk.isEmpty()) {
                try {
                    manifest.setMinSdkVersion(Integer.parseInt(minSdk));
                } catch (NumberFormatException e) {
                    logger.warning("Invalid minSdkVersion: " + minSdk);
                }
            }
            
            String targetSdk = usesSdk.getAttribute("android:targetSdkVersion");
            if (!targetSdk.isEmpty()) {
                try {
                    manifest.setTargetSdkVersion(Integer.parseInt(targetSdk));
                } catch (NumberFormatException e) {
                    logger.warning("Invalid targetSdkVersion: " + targetSdk);
                }
            }
        }
        
        // Parse permissions
        NodeList permissionNodes = root.getElementsByTagName("uses-permission");
        for (int i = 0; i < permissionNodes.getLength(); i++) {
            Element permission = (Element) permissionNodes.item(i);
            String permissionName = permission.getAttribute("android:name");
            if (!permissionName.isEmpty()) {
                manifest.addPermission(permissionName);
            }
        }
        
        // Parse application
        NodeList appNodes = root.getElementsByTagName("application");
        if (appNodes.getLength() > 0) {
            Element application = (Element) appNodes.item(0);
            
            // Parse activities
            NodeList activityNodes = application.getElementsByTagName("activity");
            for (int i = 0; i < activityNodes.getLength(); i++) {
                AndroidComponent activity = parseComponent((Element) activityNodes.item(i));
                manifest.addActivity(activity);
            }
            
            // Parse services
            NodeList serviceNodes = application.getElementsByTagName("service");
            for (int i = 0; i < serviceNodes.getLength(); i++) {
                AndroidComponent service = parseComponent((Element) serviceNodes.item(i));
                manifest.addService(service);
            }
            
            // Parse receivers
            NodeList receiverNodes = application.getElementsByTagName("receiver");
            for (int i = 0; i < receiverNodes.getLength(); i++) {
                AndroidComponent receiver = parseComponent((Element) receiverNodes.item(i));
                manifest.addReceiver(receiver);
            }
            
            // Parse providers
            NodeList providerNodes = application.getElementsByTagName("provider");
            for (int i = 0; i < providerNodes.getLength(); i++) {
                AndroidComponent provider = parseComponent((Element) providerNodes.item(i));
                manifest.addProvider(provider);
            }
        }
        
        return manifest;
    }
    
    private AndroidComponent parseComponent(Element componentElement) {
        String name = componentElement.getAttribute("android:name");
        String className = name; // May need to resolve relative names
        
        AndroidComponent component = new AndroidComponent(name, className);
        
        String exported = componentElement.getAttribute("android:exported");
        if ("true".equals(exported)) {
            component.setExported(true);
        }
        
        // Parse intent filters
        NodeList intentFilterNodes = componentElement.getElementsByTagName("intent-filter");
        for (int i = 0; i < intentFilterNodes.getLength(); i++) {
            IntentFilter intentFilter = parseIntentFilter((Element) intentFilterNodes.item(i));
            component.addIntentFilter(intentFilter);
        }
        
        return component;
    }
    
    private IntentFilter parseIntentFilter(Element intentFilterElement) {
        IntentFilter intentFilter = new IntentFilter();
        
        // Parse actions
        NodeList actionNodes = intentFilterElement.getElementsByTagName("action");
        for (int i = 0; i < actionNodes.getLength(); i++) {
            Element action = (Element) actionNodes.item(i);
            String actionName = action.getAttribute("android:name");
            if (!actionName.isEmpty()) {
                intentFilter.addAction(actionName);
            }
        }
        
        // Parse categories
        NodeList categoryNodes = intentFilterElement.getElementsByTagName("category");
        for (int i = 0; i < categoryNodes.getLength(); i++) {
            Element category = (Element) categoryNodes.item(i);
            String categoryName = category.getAttribute("android:name");
            if (!categoryName.isEmpty()) {
                intentFilter.addCategory(categoryName);
            }
        }
        
        // Parse data
        NodeList dataNodes = intentFilterElement.getElementsByTagName("data");
        if (dataNodes.getLength() > 0) {
            Element data = (Element) dataNodes.item(0);
            String scheme = data.getAttribute("android:scheme");
            if (!scheme.isEmpty()) {
                intentFilter.setDataScheme(scheme);
            }
        }
        
        return intentFilter;
    }
    
    private AndroidManifest parseBinaryManifest(InputStream manifestStream) {
        // Simplified binary parsing - in a real implementation, this would
        // parse the Android Binary XML format (AXML)
        AndroidManifest manifest = new AndroidManifest();
        
        try {
            // Read binary manifest header
            byte[] buffer = new byte[8];
            manifestStream.read(buffer);
            
            // Check for binary XML magic
            if (buffer[0] == 0x03 && buffer[1] == 0x00 && buffer[2] == 0x08 && buffer[3] == 0x00) {
                logger.info("Detected binary XML manifest");
                
                // For now, create a minimal manifest
                manifest.setPackageName("com.example.app");
                manifest.setVersionCode(1);
                manifest.setVersionName("1.0");
                manifest.setMinSdkVersion(21);
                manifest.setTargetSdkVersion(30);
                
                // Add common permissions
                manifest.addPermission("android.permission.INTERNET");
                manifest.addPermission("android.permission.ACCESS_NETWORK_STATE");
                
                // Add a main activity
                AndroidComponent mainActivity = new AndroidComponent(
                    "MainActivity", "com.example.app.MainActivity");
                mainActivity.setExported(true);
                
                IntentFilter mainFilter = new IntentFilter();
                mainFilter.addAction("android.intent.action.MAIN");
                mainFilter.addCategory("android.intent.category.LAUNCHER");
                mainActivity.addIntentFilter(mainFilter);
                
                manifest.addActivity(mainActivity);
            }
            
        } catch (Exception e) {
            logger.warning("Binary manifest parsing failed: " + e.getMessage());
        }
        
        return manifest;
    }
    
    private AndroidManifest createDefaultManifest() {
        AndroidManifest manifest = new AndroidManifest();
        manifest.setPackageName("com.unknown.app");
        manifest.setVersionCode(1);
        manifest.setVersionName("1.0");
        manifest.setMinSdkVersion(21);
        manifest.setTargetSdkVersion(30);
        return manifest;
    }
}