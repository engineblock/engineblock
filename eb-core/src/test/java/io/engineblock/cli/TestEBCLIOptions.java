package io.engineblock.cli;

import org.testng.annotations.Test;

import java.security.InvalidParameterException;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class TestEBCLIOptions {

    @Test
    public void shouldRecognizeActivities() {
        EBCLIOptions opts = new EBCLIOptions(new String[]{"activity", "foo", "activity", "bar"});
        assertThat(opts.getCommands()).isNotNull();
        assertThat(opts.getCommands().size()).isEqualTo(2);
        assertThat(opts.getCommands().get(0).getCmdSpec()).isEqualTo("foo");
        assertThat(opts.getCommands().get(1).getCmdSpec()).isEqualTo("bar");
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
}