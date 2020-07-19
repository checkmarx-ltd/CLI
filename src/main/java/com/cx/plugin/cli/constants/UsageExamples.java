package com.cx.plugin.cli.constants;

class UsageExamples {
    private static final String SCA_SCAN_TEMPLATE = "runCxConsole.cmd %s -projectname SP\\Cx\\Engine\\AST -scapathexclude .git -scalocationpath \\storage\\dir1\\subdir -scaUsername admin -scaPassword admin -scaAccount account -scahigh 1 -scamedium 2 -scalow 3";

    static final String SCAN = "\n\nCxConsole Scan -Projectname SP\\Cx\\Engine\\AST -CxServer http://localhost -cxuser admin@cx -cxpassword admin -locationtype folder -locationpath C:\\cx" +
            " -preset All -incremental -reportpdf a.pdf\nCxConsole Scan -projectname SP\\Cx\\Engine\\AST -cxserver http://localhost -cxuser admin@cx -cxpassword admin -locationtype tfs" +
            " -locationurl http://vsts2003:8080 -locationuser dm\\matys -locationpassword XYZ -preset default -reportxml a.xml -reportpdf b.pdf" +
            " -incremental -forcescan\nCxConsole Scan -projectname SP\\Cx\\Engine\\AST -cxserver http://localhost -cxuser admin@cx -cxpassword admin -locationtype share" +
            " -locationpath '\\\\storage\\path1;\\\\storage\\path2' -locationuser dm\\matys -locationpassword XYZ -preset \"Sans 25\" -reportxls a.xls -reportpdf b.pdf -private -verbose -log a.log\n" +
            " -LocationPathExclude test*, *log* -LocationFilesExclude web.config , *.class\n";

    static final String TOKEN_GEN = "runCxConsole.cmd GenerateToken -CxServer http://localhost -cxuser admin@company -cxpassword admin -v";

    static final String TOKEN_REVOKE = "runCxConsole.cmd RevokeToken -CxToken 1241513513tsfrg42 -CxServer http://localhost -v";

    static final String OSA = "\n\nrunCxConsole.cmd OsaScan -v -Projectname SP\\Cx\\Engine\\AST -CxServer http://localhost -cxuser admin -cxpassword admin -osaLocationPath C:\\cx  -OsaFilesExclude *.class OsaPathExclude src,temp  \n"
            + "runCxConsole.cmd  OsaScan -v -projectname SP\\Cx\\Engine\\AST -cxserver http://localhost -cxuser admin -cxpassword admin -locationtype folder -locationurl http://vsts2003:8080 -locationuser dm\\matys -locationpassword XYZ  \n"
            + "runCxConsole.cmd  OsaScan -v -projectname SP\\Cx\\Engine\\AST -cxserver http://localhost -cxuser admin -cxpassword admin -locationtype shared -locationpath '\\storage\\path1;\\storage\\path2' -locationuser dm\\matys -locationpassword XYZ  -log a.log\n \n"
            + "runCxConsole.cmd  OsaScan -v -Projectname CxServer\\SP\\Company\\my project -CxServer http://localhost -cxuser admin -cxpassword admin -locationtype folder -locationpath C:\\Users\\some_project -OsaFilesExclude *.bat ";

    static final String SCA_SCAN = String.format(SCA_SCAN_TEMPLATE, "ScaScan");
    static final String ASYNC_SCA_SCAN = String.format(SCA_SCAN_TEMPLATE, "AsyncScaScan");

    static final String TEST_CONNECTION = "runCxConsole.cmd TestConnection -CxServer http://localhost -usesso -v";
}
