# DavaJava Build Guide

## 🚀 Streamlined Build System

Since your Java JDK is ready, here are your build options:

## **Build Scripts**

### **1. verify-java.bat** 
**Purpose:** Verify Java JDK installation
```cmd
verify-java.bat
```
✅ **Run this first** to ensure Java is properly configured

### **2. build.bat** (Recommended)
**Purpose:** Full build with all features
```cmd
build.bat
```
**What it does:**
- Downloads all required dependencies (Commons CLI, Jackson, SLF4J, Logback)
- Compiles entire DavaJava project
- Creates `davajava-migrator.jar` executable
- Tests with example files
- Full CLI functionality

**Time:** ~2-3 minutes

### **3. quick-build.bat**
**Purpose:** Fast core functionality test
```cmd
quick-build.bat
```
**What it does:**
- Compiles core parsing logic only
- No external dependencies
- Tests Rust/Python parsing patterns
- Perfect for development and testing

**Time:** ~30 seconds

## **After Building**

### Full Build Success:
```cmd
java -jar davajava-migrator.jar --help
java -jar davajava-migrator.jar -i examples\example.rs -o output\ -l rust
```

### Quick Build Success:
```cmd
java -cp quick-build\classes TestCore
```

## **Project Structure After Build**

```
DavaJava[java]/
├── verify-java.bat          # Java verification
├── build.bat                # Full build script  
├── quick-build.bat          # Quick test build
├── davajava-migrator.jar    # Executable (after full build)
├── build\                   # Build artifacts
├── examples\                # Test files
├── src\                     # Source code
└── config\                  # Configuration templates
```

## **Troubleshooting**

### Build Fails:
1. Run `verify-java.bat` first
2. Ensure internet connection for dependency downloads
3. Try `quick-build.bat` for core functionality

### Java Issues:
- Ensure you have **JDK** (not just JRE)
- Java 11+ required
- `javac` command should work

### Network Issues:
- `build.bat` needs internet to download JARs
- `quick-build.bat` works offline

## **What's Different**

**Removed old scripts:**
- ❌ `mvnw.cmd` (Maven wrapper not needed)
- ❌ Multiple confusing build scripts
- ❌ External Maven dependency

**Clean new system:**
- ✅ Pure Java build process
- ✅ Clear purpose for each script
- ✅ Works with standard JDK installation
- ✅ No Maven required

## **Next Steps**

1. **Verify setup:** `verify-java.bat`
2. **Full build:** `build.bat` 
3. **Test migration:** `java -jar davajava-migrator.jar -i examples\example.rs -o output\ -l rust`
4. **Read usage guide:** `USAGE.md`

The build system is now streamlined and ready for your Java environment! 🎉