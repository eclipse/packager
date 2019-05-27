package org.eclipse.packager.rpm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.Test;

public class Issue29Test
{
    @Test
    public void test1 () throws IOException
    {
        try ( final RpmInputStream in = new RpmInputStream ( new BufferedInputStream ( new URL ( "https://yum.puppetlabs.com/puppet5/el/7/x86_64/puppet-agent-5.3.8-1.el7.x86_64.rpm" ).openStream () ) ) )
        {
            Dumper.dumpAll ( in );
        }

    }
}
