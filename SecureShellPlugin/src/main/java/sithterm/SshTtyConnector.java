package sithterm;

import java.nio.charset.Charset;

import com.jediterm.pty.PtyProcessTtyConnector;
import com.pty4j.PtyProcess;

public class SshTtyConnector extends PtyProcessTtyConnector {

	public SshTtyConnector(PtyProcess process, Charset charset) {
		super(process, charset);
	}

	
	
}
