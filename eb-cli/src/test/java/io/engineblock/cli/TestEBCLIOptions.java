package io.engineblock.cli;

import org.testng.annotations.Test;

import java.security.InvalidParameterException;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class TestEBCLIOptions {

    @Test
    public void shouldRecognizeActivities() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"activity", "foo=wan", "activity", "bar=lan"});
        assertThat(opts.getCommands()).isNotNull();
        assertThat(opts.getCommands().size()).isEqualTo(2);
        assertThat(opts.getCommands().get(0).getCmdSpec()).isEqualTo("foo=wan;");
        assertThat(opts.getCommands().get(1).getCmdSpec()).isEqualTo("bar=lan;");
    }

    @Test
    public void shouldParseLongActivityForm() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"activity", "param1=param2", "param3=param4", "report-graphite-to", "woot"});
        assertThat(opts.getCommands().size()).isEqualTo(1);
        assertThat(opts.getCommands().get(0).getCmdSpec()).isEqualTo("param1=param2;param3=param4;");
        assertThat(opts.wantsReportGraphiteTo()).isEqualTo("woot");
    }

    @Test
    public void shouldRecognizeVersion() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"version"});
        assertThat(opts.wantsVersion()).isTrue();
    }

    @Test
    public void shouldRecognizeScripts() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"script", "ascriptaone", "script", "ascriptatwo"});
        assertThat(opts.getCommands()).isNotNull();
        assertThat(opts.getCommands().size()).isEqualTo(2);
        assertThat(opts.getCommands().get(0).getCmdType()).isEqualTo(EBCLIOptions.CmdType.script);
        assertThat(opts.getCommands().get(0).getCmdSpec()).isEqualTo("ascriptaone");
        assertThat(opts.getCommands().get(1).getCmdType()).isEqualTo(EBCLIOptions.CmdType.script);
        assertThat(opts.getCommands().get(1).getCmdSpec()).isEqualTo("ascriptatwo");
    }

    @Test
    public void shouldRecognizeWantsActivityTypes() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"activitytypes"});
        assertThat(opts.wantsActivityTypes()).isTrue();
        opts = new EBCLIOptions(new String[]{"version"});
        assertThat(opts.wantsActivityTypes()).isFalse();
    }

    @Test
    public void shouldRecognizeWantsBasicHelp() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"help"});
        assertThat(opts.wantsBasicHelp()).isTrue();
        opts = new EBCLIOptions(new String[]{"version"});
        assertThat(opts.wantsActivityHelp()).isFalse();
    }

    @Test
    public void shouldRecognizeWantsActivityHelp() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"help", "foo"});
        assertThat(opts.wantsActivityHelp()).isTrue();
        assertThat(opts.wantsActivityHelpFor()).isEqualTo("foo");
        opts = new EBCLIOptions(new String[]{"version"});
        assertThat(opts.wantsActivityHelp()).isFalse();
    }

    @Test(expectedExceptions = {InvalidParameterException.class}, expectedExceptionsMessageRegExp = ".*unrecognized command.*")
    public void shouldErrorSanelyWhenNoMatch() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"unrecognizable command"});
    }

    @Test
    public void testShouldRecognizeScriptParams() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"script", "ascript", "param1=value1"});
        assertThat(opts.getCommands().size()).isEqualTo(1);
        EBCLIOptions.Cmd cmd = opts.getCommands().get(0);
        assertThat(cmd.getCmdArgs().size()).isEqualTo(1);
        assertThat(cmd.getCmdArgs()).containsKey("param1");
        assertThat(cmd.getCmdArgs().get("param1")).isEqualTo("value1");
    }

    @Test(expectedExceptions = {InvalidParameterException.class},
            expectedExceptionsMessageRegExp = ".*script name must precede.*")
    public void testShouldErrorSanelyWhenScriptNameSkipped() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"script", "param1=value1"});
    }

    @Test(expectedExceptions = {InvalidParameterException.class},
    expectedExceptionsMessageRegExp = ".*missing script name.*")
    public void testShouldErrorForMissingScriptName() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"script"});
    }

    @Test
    public void testScriptInterpolation() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"script", "script_to_interpolate", "parameter1=replaced"});
        String s = EBCLIScriptAssembly.assembleScript(opts);
        assertThat(s).contains("var foo=replaced;");
        assertThat(s).contains("var bar=UNSET:parameter2");
    }

}