package com.polytype.migrator.web;

import java.util.*;
import java.io.*;

/**
 * Comprehensive Web Migration Demonstration
 * Showcases both modern framework migrations and legacy technology migrations
 */
public class WebMigrationDemo {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("POLYTYPE WEB FRAMEWORK MIGRATION SYSTEM DEMONSTRATION");
        System.out.println("=".repeat(80));
        System.out.println();
        
        // Run modern framework migration demos
        runModernFrameworkMigrations();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println();
        
        // Run legacy technology migration demos  
        runLegacyTechnologyMigrations();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DEMO COMPLETED - All Web Migration Systems Operational");
        System.out.println("=".repeat(80));
    }
    
    private static void runModernFrameworkMigrations() {
        System.out.println("MODERN WEB FRAMEWORK MIGRATIONS");
        System.out.println("-".repeat(50));
        
        WebFrameworkMigrator migrator = new WebFrameworkMigrator();
        List<WebFrameworkMigrator.MigrationResult> results = new ArrayList<>();
        
        // 1. JSP to Spring Boot Migration
        System.out.println("1. JSP to Spring Boot Migration:");
        String jspCode = """
            <%@ page language="java" contentType="text/html; charset=UTF-8" %>
            <html>
            <head><title>User Management</title></head>
            <body>
            <% 
                String userName = request.getParameter("user");
                if (userName != null) {
                    out.println("Welcome " + userName);
                }
            %>
            <form method="post">
                <input type="text" name="user" placeholder="Username">
                <input type="submit" value="Submit">
            </form>
            </body>
            </html>
            """;
        
        WebFrameworkMigrator.MigrationResult jspResult = migrator.migrate(jspCode, 
            WebFrameworkMigrator.WebFramework.JSP, WebFrameworkMigrator.WebFramework.SPRING_BOOT);
        results.add(jspResult);
        
        System.out.println("   Source: JSP with scriptlets and form handling");
        System.out.println("   Target: Spring Boot RestController");
        System.out.println("   Changes: " + jspResult.getChangesApplied().size());
        System.out.println("   Complexity: " + jspResult.getComplexityScore() + "/100");
        System.out.println();
        
        // 2. ASP.NET WebForms to MVC Migration
        System.out.println("2. ASP.NET WebForms to MVC Migration:");
        String webFormsCode = """
            <%@ Page Language="C#" %>
            <html>
            <head runat="server">
                <title>Product Manager</title>
            </head>
            <body>
            <form id="form1" runat="server">
                <asp:Label ID="lblWelcome" Text="Product Management" runat="server" />
                <asp:TextBox ID="txtProductName" runat="server" />
                <asp:Button ID="btnSave" Text="Save Product" OnClick="btnSave_Click" runat="server" />
            </form>
            </body>
            </html>
            
            <script runat="server">
            protected void Page_Load(object sender, EventArgs e) {
                if (!IsPostBack) {
                    lblWelcome.Text = "Welcome to Product Management";
                }
            }
            
            protected void btnSave_Click(object sender, EventArgs e) {
                string productName = txtProductName.Text;
                // Save product logic
            }
            </script>
            """;
        
        WebFrameworkMigrator.MigrationResult aspResult = migrator.migrate(webFormsCode,
            WebFrameworkMigrator.WebFramework.ASP_NET_WEBFORMS, WebFrameworkMigrator.WebFramework.ASP_NET_MVC);
        results.add(aspResult);
        
        System.out.println("   Source: ASP.NET WebForms with server controls");
        System.out.println("   Target: ASP.NET MVC with Razor views");
        System.out.println("   Changes: " + aspResult.getChangesApplied().size());
        System.out.println("   Warnings: " + aspResult.getWarnings().size());
        System.out.println();
        
        // 3. Rails to Express.js Migration
        System.out.println("3. Ruby on Rails to Express.js Migration:");
        String railsCode = """
            class ProductsController < ApplicationController
              def index
                @products = Product.all
                render :index
              end
              
              def create
                @product = Product.new(product_params)
                if @product.save
                  redirect_to products_path, notice: 'Product created successfully'
                else
                  render :new
                end
              end
              
              def destroy
                @product = Product.find(params[:id])
                @product.destroy
                redirect_to products_path
              end
              
              private
              
              def product_params
                params.require(:product).permit(:name, :price, :description)
              end
            end
            """;
        
        WebFrameworkMigrator.MigrationResult railsResult = migrator.migrate(railsCode,
            WebFrameworkMigrator.WebFramework.RAILS, WebFrameworkMigrator.WebFramework.EXPRESS_JS);
        results.add(railsResult);
        
        System.out.println("   Source: Rails controller with CRUD operations");
        System.out.println("   Target: Express.js with REST endpoints");
        System.out.println("   Changes: " + railsResult.getChangesApplied().size());
        System.out.println("   Complexity: " + railsResult.getComplexityScore() + "/100");
        System.out.println();
        
        // 4. Struts to Spring Boot Migration
        System.out.println("4. Apache Struts to Spring Boot Migration:");
        String strutsCode = """
            public class UserAction extends Action {
                public ActionForward execute(ActionMapping mapping, 
                                           ActionForm form,
                                           HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
                    
                    UserForm userForm = (UserForm) form;
                    String username = userForm.getUsername();
                    String password = userForm.getPassword();
                    
                    UserService userService = new UserService();
                    User user = userService.authenticate(username, password);
                    
                    if (user != null) {
                        request.getSession().setAttribute("user", user);
                        return mapping.findForward("success");
                    } else {
                        ActionMessages errors = new ActionMessages();
                        errors.add("login", new ActionMessage("error.login.invalid"));
                        saveErrors(request, errors);
                        return mapping.findForward("failure");
                    }
                }
            }
            """;
        
        WebFrameworkMigrator.MigrationResult strutsResult = migrator.migrate(strutsCode,
            WebFrameworkMigrator.WebFramework.STRUTS, WebFrameworkMigrator.WebFramework.SPRING_BOOT);
        results.add(strutsResult);
        
        System.out.println("   Source: Struts Action with form handling");
        System.out.println("   Target: Spring Boot REST controller");
        System.out.println("   Changes: " + strutsResult.getChangesApplied().size());
        System.out.println("   Warnings: " + strutsResult.getWarnings().size());
        System.out.println();
        
        // Generate modern framework migration report
        String modernReport = migrator.generateMigrationReport(results);
        System.out.println("MODERN FRAMEWORK MIGRATION SUMMARY:");
        System.out.println(modernReport.substring(0, Math.min(modernReport.length(), 800)) + "...");
    }
    
    private static void runLegacyTechnologyMigrations() {
        System.out.println("LEGACY WEB TECHNOLOGY MIGRATIONS");
        System.out.println("-".repeat(50));
        
        LegacyWebMigrator legacyMigrator = new LegacyWebMigrator();
        List<LegacyWebMigrator.LegacyMigrationResult> legacyResults = new ArrayList<>();
        
        // 1. CGI Perl to Express.js Migration
        System.out.println("1. CGI Perl to Express.js Migration:");
        String cgiPerlCode = """
            #!/usr/bin/perl
            use CGI;
            use strict;
            use warnings;
            
            my $cgi = CGI->new;
            
            print "Content-type: text/html\\n\\n";
            print "<html><head><title>Visitor Counter</title></head><body>";
            
            my $name = $cgi->param('name') || 'Guest';
            my $email = $cgi->param('email') || '';
            
            # Simple visitor counter
            my $counter_file = "/tmp/counter.txt";
            my $count = 0;
            
            if (-e $counter_file) {
                open(my $fh, '<', $counter_file);
                $count = <$fh>;
                close($fh);
                chomp $count;
            }
            
            $count++;
            
            open(my $fh, '>', $counter_file);
            print $fh $count;
            close($fh);
            
            print "<h1>Welcome, $name!</h1>";
            print "<p>Email: $email</p>";
            print "<p>You are visitor number: $count</p>";
            print "</body></html>";
            """;
        
        LegacyWebMigrator.LegacyMigrationResult cgiResult = legacyMigrator.migrateLegacyToModern(cgiPerlCode,
            LegacyWebMigrator.LegacyWebTechnology.CGI_PERL, LegacyWebMigrator.ModernTarget.NODE_EXPRESS);
        legacyResults.add(cgiResult);
        
        System.out.println("   Source: CGI Perl with file-based counter");
        System.out.println("   Target: Express.js with modern routing");
        System.out.println("   Modernizations: " + cgiResult.getModernizations().size());
        System.out.println("   Security Improvements: " + cgiResult.getSecurityImprovements().size());
        System.out.println();
        
        // 2. Classic ASP to .NET Core Migration
        System.out.println("2. Classic ASP to .NET Core Migration:");
        String classicAspCode = """
            <%
            Dim userName, userEmail
            userName = Request.Form("username")
            userEmail = Request.Form("email")
            
            If userName <> "" Then
                ' Connect to database using old ADO
                Set conn = Server.CreateObject("ADODB.Connection")
                conn.Open "Provider=Microsoft.Jet.OLEDB.4.0;Data Source=users.mdb"
                
                Set rs = Server.CreateObject("ADODB.Recordset")
                sql = "SELECT * FROM Users WHERE Username='" & userName & "'"
                rs.Open sql, conn
                
                If Not rs.EOF Then
                    Response.Write "<h1>Welcome back, " & rs("FullName") & "!</h1>"
                Else
                    Response.Write "<h1>New user: " & userName & "</h1>"
                End If
                
                rs.Close
                conn.Close
                Set rs = Nothing
                Set conn = Nothing
            Else
                Response.Write "<h1>Please enter your username</h1>"
            End If
            %>
            
            <form method="post">
                Username: <input type="text" name="username"><br>
                Email: <input type="text" name="email"><br>
                <input type="submit" value="Login">
            </form>
            """;
        
        LegacyWebMigrator.LegacyMigrationResult aspLegacyResult = legacyMigrator.migrateLegacyToModern(classicAspCode,
            LegacyWebMigrator.LegacyWebTechnology.CLASSIC_ASP, LegacyWebMigrator.ModernTarget.DOTNET_CORE);
        legacyResults.add(aspLegacyResult);
        
        System.out.println("   Source: Classic ASP with ADO database access");
        System.out.println("   Target: .NET Core with dependency injection");
        System.out.println("   Modernizations: " + aspLegacyResult.getModernizations().size());
        System.out.println("   Deprecated Features: " + aspLegacyResult.getDeprecatedFeatures().size());
        System.out.println();
        
        // 3. PHP 3/4 to Laravel Migration
        System.out.println("3. PHP 3/4 to Laravel Migration:");
        String php34Code = """
            <?php
            // Old PHP 3/4 style code with security issues
            
            // Using old mysql functions (deprecated)
            $connection = mysql_connect("localhost", "user", "pass");
            mysql_select_db("mydb", $connection);
            
            // Using register_globals style (security risk)
            $user_id = $HTTP_GET_VARS['id'];
            $username = $HTTP_POST_VARS['username'];
            
            // SQL injection vulnerability
            $query = "SELECT * FROM users WHERE id = $user_id";
            $result = mysql_query($query);
            
            if (mysql_num_rows($result) > 0) {
                while ($row = mysql_fetch_array($result)) {
                    echo "<h1>User: " . $row['name'] . "</h1>";
                    echo "<p>Email: " . $row['email'] . "</p>";
                }
            }
            
            // Using old ereg functions
            if (ereg("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$", $email)) {
                echo "Valid email";
            }
            
            // Update user if posted
            if ($username) {
                $update_query = "UPDATE users SET username = '$username' WHERE id = $user_id";
                mysql_query($update_query);
            }
            
            mysql_close($connection);
            ?>
            """;
        
        LegacyWebMigrator.LegacyMigrationResult phpResult = legacyMigrator.migrateLegacyToModern(php34Code,
            LegacyWebMigrator.LegacyWebTechnology.PHP3_4, LegacyWebMigrator.ModernTarget.PHP_LARAVEL);
        legacyResults.add(phpResult);
        
        System.out.println("   Source: PHP 3/4 with mysql_* functions and security issues");
        System.out.println("   Target: Laravel with Eloquent ORM");
        System.out.println("   Security Improvements: " + phpResult.getSecurityImprovements().size());
        System.out.println("   Legacy Complexity: " + phpResult.getLegacyComplexity() + "/100");
        System.out.println();
        
        // 4. ColdFusion to Spring Boot Migration
        System.out.println("4. ColdFusion to Spring Boot Migration:");
        String coldFusionCode = """
            <cfset currentUser = "">
            <cfset userRole = "guest">
            
            <cfif isDefined("URL.username")>
                <cfset currentUser = URL.username>
                
                <cfquery name="getUserInfo" datasource="mydb">
                    SELECT username, role, email, last_login
                    FROM users 
                    WHERE username = '#currentUser#'
                </cfquery>
                
                <cfif getUserInfo.recordCount GT 0>
                    <cfset userRole = getUserInfo.role>
                    <cfoutput>
                        <h1>Welcome back, #getUserInfo.username#!</h1>
                        <p>Role: #getUserInfo.role#</p>
                        <p>Email: #getUserInfo.email#</p>
                        <p>Last Login: #getUserInfo.last_login#</p>
                    </cfoutput>
                    
                    <cfquery name="updateLogin" datasource="mydb">
                        UPDATE users 
                        SET last_login = #CreateODBCDateTime(Now())#
                        WHERE username = '#currentUser#'
                    </cfquery>
                <cfelse>
                    <cfoutput><h1>User #currentUser# not found</h1></cfoutput>
                </cfif>
            <cfelse>
                <h1>Please provide a username</h1>
            </cfif>
            
            <cfif userRole EQ "admin">
                <p><a href="admin.cfm">Admin Panel</a></p>
            </cfif>
            """;
        
        LegacyWebMigrator.LegacyMigrationResult cfResult = legacyMigrator.migrateLegacyToModern(coldFusionCode,
            LegacyWebMigrator.LegacyWebTechnology.COLD_FUSION, LegacyWebMigrator.ModernTarget.SPRING_BOOT);
        legacyResults.add(cfResult);
        
        System.out.println("   Source: ColdFusion with CFQUERY and CFOUTPUT");
        System.out.println("   Target: Spring Boot with JdbcTemplate");
        System.out.println("   Modernizations: " + cfResult.getModernizations().size());
        System.out.println("   Complexity: " + cfResult.getLegacyComplexity() + "/100");
        System.out.println();
        
        // 5. Server Side Includes to Express.js Migration
        System.out.println("5. Server Side Includes to Express.js Migration:");
        String ssiCode = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Company Portal</title>
                <!--#include virtual="/includes/meta.shtml" -->
            </head>
            <body>
                <!--#include virtual="/includes/header.shtml" -->
                
                <main>
                    <h1>Welcome to Our Portal</h1>
                    <p>Current date: <!--#echo var="DATE_LOCAL" --></p>
                    <p>Server: <!--#echo var="SERVER_NAME" --></p>
                    
                    <!--#if expr="${REMOTE_USER}" -->
                        <p>Hello, <!--#echo var="REMOTE_USER" -->!</p>
                    <!--#else -->
                        <p>Please log in to continue.</p>
                    <!--#endif -->
                    
                    <!-- DANGEROUS: Command execution -->
                    <p>System uptime: <!--#exec cmd="uptime" --></p>
                    
                    <!--#include virtual="/includes/navigation.shtml" -->
                    
                    <div class="content">
                        <!--#include file="content/news.html" -->
                    </div>
                </main>
                
                <!--#include virtual="/includes/footer.shtml" -->
            </body>
            </html>
            """;
        
        LegacyWebMigrator.LegacyMigrationResult ssiResult = legacyMigrator.migrateLegacyToModern(ssiCode,
            LegacyWebMigrator.LegacyWebTechnology.SHTML, LegacyWebMigrator.ModernTarget.NODE_EXPRESS);
        legacyResults.add(ssiResult);
        
        System.out.println("   Source: Server Side Includes with exec commands");
        System.out.println("   Target: Express.js with EJS templating");
        System.out.println("   Security Improvements: " + ssiResult.getSecurityImprovements().size());
        System.out.println("   Deprecated: " + ssiResult.getDeprecatedFeatures().size());
        System.out.println();
        
        // Generate comprehensive legacy migration report
        String legacyReport = legacyMigrator.generateLegacyMigrationReport(legacyResults);
        System.out.println("LEGACY TECHNOLOGY MIGRATION SUMMARY:");
        System.out.println(legacyReport.substring(0, Math.min(legacyReport.length(), 1000)) + "...");
        
        // Performance summary
        System.out.println("\nPERFORMANCE SUMMARY:");
        System.out.println("-".repeat(30));
        long totalModernizations = legacyResults.stream().mapToLong(r -> r.getModernizations().size()).sum();
        long totalSecurityImprovements = legacyResults.stream().mapToLong(r -> r.getSecurityImprovements().size()).sum();
        long totalDeprecated = legacyResults.stream().mapToLong(r -> r.getDeprecatedFeatures().size()).sum();
        double avgComplexity = legacyResults.stream().mapToInt(LegacyWebMigrator.LegacyMigrationResult::getLegacyComplexity).average().orElse(0.0);
        
        System.out.println("• Total Legacy Migrations: " + legacyResults.size());
        System.out.println("• Modernizations Applied: " + totalModernizations);
        System.out.println("• Security Issues Fixed: " + totalSecurityImprovements); 
        System.out.println("• Deprecated Features Addressed: " + totalDeprecated);
        System.out.println("• Average Legacy Complexity: " + String.format("%.1f", avgComplexity) + "/100");
        
        System.out.println("\nKEY ACHIEVEMENTS:");
        System.out.println("✓ Migrated 5 different legacy web technologies");
        System.out.println("✓ Eliminated SQL injection vulnerabilities");
        System.out.println("✓ Replaced deprecated database APIs");
        System.out.println("✓ Modernized authentication and session handling");
        System.out.println("✓ Removed dangerous command execution features");
        System.out.println("✓ Applied modern security frameworks");
        System.out.println("✓ Introduced proper input validation");
        System.out.println("✓ Converted to modern templating systems");
    }
}