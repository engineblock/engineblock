package com.metawiring.load.cli;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class TestEMCLIOptions {

    @Test
    public void shouldRecognizeActivities() {
        EMCLIOptions opts = EMCLIOptions.parse(new String[]{"--activity=foo", "--activity=bar"});
        assertThat(opts.getActivities()).isNotNull();
        assertThat(opts.getActivities().size()).isEqualTo(2);
        assertThat(opts.getActivities().get(0).getAlias()).isEqualTo("foo");
        assertThat(opts.getActivities().get(1).getAlias()).isEqualTo("bar");
    }

    @Test
    public void shouldRecognizeVersion() {
        EMCLIOptions opts = EMCLIOptions.parse(new String[]{"-v"});
        assertThat(opts.wantsVersion()).isTrue();
        assertThat(opts.wantsFullVersion()).isFalse();
    }

    @Test
    public void shouldRecognizedVerboseVersion() {
        EMCLIOptions opts = EMCLIOptions.parse(new String[]{"-vv"});
        assertThat(opts.wantsVersion()).isFalse();
        assertThat(opts.wantsFullVersion()).isTrue();
    }

    @Test
    public void shouldRecognizeScripts() {
        EMCLIOptions opts = EMCLIOptions.parse(new String[]{"ascriptaone", "ascriptatwo"});
        assertThat(opts.getScripts()).isNotNull();
        assertThat(opts.getScripts().size()).isEqualTo(2);
        assertThat(opts.getScripts().get(0)).isEqualTo("ascriptaone");
        assertThat(opts.getScripts().get(1)).isEqualTo("ascriptatwo");
    }

    @Test
    public void shouldRecognizeWantsActivityTypes() {
        EMCLIOptions opts = EMCLIOptions.parse(new String[]{"--activity-types"});
        assertThat(opts.wantsActivityTypes()).isTrue();
        opts = EMCLIOptions.parse(new String[]{"foo"});
        assertThat(opts.wantsActivityTypes()).isFalse();
    }
}