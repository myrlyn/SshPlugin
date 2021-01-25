package sithterm;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.jediterm.terminal.ui.JediTermWidget;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;

public class SecureShellPlugin extends SithTermPlugin {
	private static final String SECURE_SHELL_PLUGIN_FILES = "SecureShellPluginFiles";
	private static final String DOT_SITH = ".Sith";
	private int tabNumber = 1;
	private JPanel sshSettingsPanel = new JPanel();
	private JTabbedPane sshTabs = new JTabbedPane();
	private static final String[] intFiles = { "msys-2.0.dll", "msys-asn1-8.dll", "msys-com_err-1.dll", "msys-crypt-0.dll",
			"msys-crypto-1.0.0.dll", "msys-gcc_s-seh-1.dll", "msys-gssapi-3.dll", "msys-heimbase-1.dll",
			"msys-heimntlm-0.dll", "msys-hx509-5.dll", "msys-krb5-26.dll", "msys-roken-18.dll",
			"msys-sqlite3-0.dll", "msys-ssp-0.dll", "msys-wind-0.dll", "msys-z.dll", "ssh.exe", };
	public static void main(String[] args) {//used during testing only

		SithTermPlugin s = new SecureShellPlugin(new SithTermMainWindow());
		s.getApplication().getFrame().setVisible(true);
		s.getApplication().getPluginMapV1().put(s.getPluginName(), s);
		s.initialize("");
	}

	private static final long serialVersionUID = 1L;
	private Map<String, Serializable> mySettings = new HashMap<>();

	public SecureShellPlugin(SithTermMainWindow application) {
		super(application);
	}

	@Override
	public void applySettings() {
		// TODO do this
	}

	@Override
	public void initialize(String jarName) {
		@SuppressWarnings("unchecked")
		Map<String, Serializable> tmpMySettings = (Map<String, Serializable>) this.getApplication().getSettings()
				.getPluginSettings().get(this.getPluginName());
		boolean typeSafe = checkPluginSettingsTypeSafety(tmpMySettings);
		if (typeSafe) {
			mySettings = tmpMySettings;
		}
		JMenuItem newSshTab = new JMenuItem("New SSH Tab");
		this.getApplication().getMnTabs().add(newSshTab);
		newSshTab.addActionListener(evt -> {
			Component c = getWidget();
			this.getApplication().getTabbedPane().addTab("SSH: " + tabNumber++, c);
			this.getApplication().getTabbedPane().setSelectedComponent(c);
		});
		
		if ((null == jarName) || "".equalsIgnoreCase(jarName)) {
			extractFilesAsResources();
		} else {
			extractFilesFromJar(jarName);
		}
		File filesDir = new File( this.getApplication().getSettings().getDir()
		+ File.separator + DOT_SITH + File.separator + SECURE_SHELL_PLUGIN_FILES);
		if (!filesDir.exists()) {
			filesDir.mkdirs();
		}
		// TODO add ssh settings to settings UI 
		populateSettingsWindow();
		//TODO add save/restore sessions for ssh sessions
	}

	private void populateSettingsWindow() {
		SettingsPopup pop = this.getApplication().getSpop();
		if (null == pop) {
			pop = new SettingsPopup("JediTerm Settings", this.getApplication());
			pop.setBounds(50, 50, 700, 700);
			this.getApplication().setSpop(pop);
		}
		pop.getTabbedPane().add("SSH Plugin", sshSettingsPanel);
		sshSettingsPanel.add( sshTabs);
		JPanel connectionsPane = new JPanel();
		connectionsPane.setLayout(new GridBagLayout());
		sshTabs.addTab("Connection Settings", connectionsPane);
		
	}

	public int getTabNumber() {
		return tabNumber;
	}

	public void setTabNumber(int tabNumber) {
		this.tabNumber = tabNumber;
	}

	public JPanel getSshSettingsPanel() {
		return sshSettingsPanel;
	}

	public void setSshSettingsPanel(JPanel sshSettingsPanel) {
		this.sshSettingsPanel = sshSettingsPanel;
	}

	public Map<String, Serializable> getMySettings() {
		return mySettings;
	}

	public void setMySettings(Map<String, Serializable> mySettings) {
		this.mySettings = mySettings;
	}

	public static String getSecureShellPluginFiles() {
		return SECURE_SHELL_PLUGIN_FILES;
	}

	public static String getDotSith() {
		return DOT_SITH;
	}

	public static String[] getIntfiles() {
		return intFiles;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	private void extractFilesFromJar(String jarName) {
		try (JarFile jar = new JarFile(new File(jarName));) {

			
			for (String s : intFiles) {
				JarEntry jarEntry = jar.getJarEntry(s);

				if (jarEntry != null) {
					try (ReadableByteChannel rbc = Channels
							.newChannel(new URL("jar:file:" + jarName + "!/" + s).openStream())) {
						try (FileOutputStream fos = new FileOutputStream(this.getApplication().getSettings().getDir()
								+ File.separator + DOT_SITH + File.separator +  SECURE_SHELL_PLUGIN_FILES+ File.separator+s)) {
							fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
							fos.flush();
						}
					}
				}
			}
		} catch (IOException e) {
			SithTermMainWindow.getLogger().error("Could not open jar");
		}
	}

	private void extractFilesAsResources() {
		try {
		
			for (String s : intFiles) {
				Enumeration<URL> res = Thread.currentThread().getContextClassLoader().getResources(s);
				while (res.hasMoreElements()) {
					URL resUrl = res.nextElement();
					SithTermMainWindow.getLogger().info("URL: " + resUrl.toString());

					try (ReadableByteChannel rbc = Channels.newChannel(resUrl.openStream())) {
						try (FileOutputStream fos = new FileOutputStream(this.getApplication().getSettings().getDir()
								+ File.separator + DOT_SITH + File.separator +SECURE_SHELL_PLUGIN_FILES+ File.separator+s)) {
							fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
							fos.flush();
						}
					}
				}
			}
		} catch (IOException e) {
			SithTermMainWindow.getLogger().error("Could not find SSH executable", e);
		}
	}

	private boolean checkPluginSettingsTypeSafety(Map<String, Serializable> tmpMySettings) {
		boolean typeSafe = true;
		if (null != tmpMySettings && Map.class.isAssignableFrom(tmpMySettings.getClass())) {
			for (Entry<?, ?> e : tmpMySettings.entrySet()) {
				if (!((e.getKey() instanceof String)
						&& (Serializable.class.isAssignableFrom(e.getValue().getClass())))) {
					typeSafe = false;
					break;
				}
			}
		}
		return typeSafe;
	}

	private Component getWidget() {
		JediTermWidget jtw = new JediTermWidget(new SithSettingsProvider(this.getApplication().getSettings()));
		jtw.setBackground(this.getApplication().getSettings().getBgColor());
		jtw.setForeground(this.getApplication().getSettings().getFgColor());
		JDialog optsDialog = new JDialog();
		this.getApplication().getFrame().setModalExclusionType(Dialog.ModalExclusionType.NO_EXCLUDE);
		optsDialog.setModal(true);
		optsDialog.setTitle("Connection Details");
		optsDialog.setSize(800, 600);
		optsDialog.setLayout(new GridBagLayout());
		Container optsPane = optsDialog.getContentPane();
		GridBagConstraints gbc_usernameLabel = new GridBagConstraints();
		GridBagLayout layout = new GridBagLayout();
		int[] widths = { 160, 240, 80, 80, 80, 80, 80 };
		int[] heights = { 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
				20, 20 };
		layout.columnWidths = widths;
		layout.rowHeights = heights;
		optsPane.setLayout(layout);
		gbc_usernameLabel.gridx = 0;
		gbc_usernameLabel.gridy = 0;
		gbc_usernameLabel.anchor = GridBagConstraints.WEST;
		gbc_usernameLabel.fill = GridBagConstraints.HORIZONTAL;
		optsPane.add(new JLabel("Username"), gbc_usernameLabel);

		JTextField userNameField = new JTextField();
		userNameField.setText(System.getProperty("user.name"));
		GridBagConstraints gbc_usernameField = new GridBagConstraints();
		gbc_usernameField.anchor = GridBagConstraints.WEST;
		gbc_usernameField.gridx = 1;
		gbc_usernameField.gridy = 0;
		gbc_usernameField.fill = GridBagConstraints.HORIZONTAL;
		optsPane.add(userNameField, gbc_usernameField);

		JLabel envLabel = new JLabel("Environment Variables");
		GridBagConstraints gbc_EnvLabel = new GridBagConstraints();
		gbc_EnvLabel.gridx = 0;
		gbc_EnvLabel.gridy = 1;
		gbc_EnvLabel.anchor = GridBagConstraints.WEST;
		gbc_EnvLabel.fill = GridBagConstraints.HORIZONTAL;
		envLabel.setToolTipText("Comma-separated key=value pairs");
		optsPane.add(envLabel, gbc_EnvLabel);

		JTextArea envField = new JTextArea();
		envField.setText("");
		GridBagConstraints gbc_EnvField = new GridBagConstraints();
		gbc_EnvField.gridx = 1;
		gbc_EnvField.gridy = 1;
		gbc_EnvField.anchor = GridBagConstraints.WEST;
		gbc_EnvField.fill = GridBagConstraints.HORIZONTAL;
		optsPane.add(envField, gbc_EnvField);

		JLabel hostLabel = new JLabel("Host");
		GridBagConstraints gbc_HostLabel = new GridBagConstraints();
		gbc_HostLabel.gridx = 0;
		gbc_HostLabel.gridy = 2;
		gbc_HostLabel.anchor = GridBagConstraints.WEST;
		gbc_HostLabel.fill = GridBagConstraints.HORIZONTAL;
		optsPane.add(hostLabel, gbc_HostLabel);

		JTextField hostField = new JTextField();
		GridBagConstraints gbc_HostField = new GridBagConstraints();
		gbc_HostField.gridx = 1;
		gbc_HostField.gridy = 2;
		gbc_HostField.anchor = GridBagConstraints.WEST;
		gbc_HostField.fill = GridBagConstraints.HORIZONTAL;
		optsPane.add(hostField, gbc_HostField);

		JLabel keyLabel = new JLabel("ID File");
		GridBagConstraints gbc_keyLabel = new GridBagConstraints();
		gbc_keyLabel.gridx = 0;
		gbc_keyLabel.gridy = 3;
		gbc_keyLabel.anchor = GridBagConstraints.WEST;
		gbc_keyLabel.fill = GridBagConstraints.HORIZONTAL;
		optsPane.add(keyLabel, gbc_keyLabel);

		String homeDir = System.getProperty("user.home");
		String sshdir = homeDir + File.separator + ".ssh";
		String idfile = sshdir + File.separator + "id_rsa";
		String khfile = sshdir + File.separator + "known_hosts";

		JTextField keyTextField = new JTextField();
		keyTextField.setText(idfile);
		GridBagConstraints gbc_keyField = new GridBagConstraints();
		gbc_keyField.gridx = 1;
		gbc_keyField.gridy = 3;
		gbc_keyField.anchor = GridBagConstraints.WEST;
		gbc_keyField.fill = GridBagConstraints.HORIZONTAL;
		optsPane.add(keyTextField, gbc_keyField);
		keyTextField.setText("");

		JLabel knownHostsLabel = new JLabel("Known Hosts");
		GridBagConstraints gbc_khostsLabel = new GridBagConstraints();
		gbc_khostsLabel.gridx = 0;
		gbc_khostsLabel.gridy = 4;
		gbc_khostsLabel.anchor = GridBagConstraints.WEST;
		gbc_khostsLabel.fill = GridBagConstraints.HORIZONTAL;
		optsPane.add(knownHostsLabel, gbc_khostsLabel);

		JTextField knownHostsField = new JTextField();
		knownHostsField.setText(khfile);
		GridBagConstraints gbc_khField = new GridBagConstraints();
		gbc_khField.gridx = 1;
		gbc_khField.gridy = 4;
		gbc_khField.anchor = GridBagConstraints.WEST;
		gbc_khField.fill = GridBagConstraints.HORIZONTAL;
		optsPane.add(knownHostsField, gbc_khField);
		knownHostsField.setText("");

		JLabel portLabel = new JLabel("Port");
		GridBagConstraints gbc_portLabel = new GridBagConstraints();
		gbc_portLabel.gridx = 0;
		gbc_portLabel.gridy = 5;
		gbc_portLabel.anchor = GridBagConstraints.WEST;
		gbc_portLabel.fill = GridBagConstraints.HORIZONTAL;
		optsPane.add(portLabel, gbc_portLabel);

		JTextField portField = new JTextField();
		portField.setText(khfile);
		GridBagConstraints gbc_portField = new GridBagConstraints();
		gbc_portField.gridx = 1;
		gbc_portField.gridy = 5;
		gbc_portField.anchor = GridBagConstraints.WEST;
		gbc_portField.fill = GridBagConstraints.HORIZONTAL;
		optsPane.add(portField, gbc_portField);
		portField.setText("22");

		JButton connectButton = new JButton("Connect");
		GridBagConstraints buttonConstraints = new GridBagConstraints();
		buttonConstraints.gridx = 0;
		buttonConstraints.gridy = 7;
		buttonConstraints.anchor = GridBagConstraints.WEST;
		buttonConstraints.fill = GridBagConstraints.HORIZONTAL;
		optsPane.add(connectButton, buttonConstraints);
		// TODO [-46AaCfGgKkMNnqsTtVvXxYy] [-B bind_interface] [-b bind_address] [-c
		// cipher_spec] [-D [bind_address:]port] [-E log_file] [-e escape_char] [-F
		// configfile] [-I pkcs11] [-J destination] [-L address] [-m mac_spec] [-O
		// ctl_cmd] [-o option] [-Q query_option] [-R address] [-S ctl_path] [-W
		// host:port] [-w local_tun[:remote_tun]]
		connectButton.addActionListener(evt -> {
			String exe = this.getApplication().getSettings().getDir() + File.separator + DOT_SITH + File.separator
					+ SECURE_SHELL_PLUGIN_FILES+File.separator+"ssh.exe";
			List<String> command = new LinkedList<>();
			command.add(exe);
			command.add("-p");
			command.add(portField.getText());
			command.add("-l");
			command.add(userNameField.getText());
			// TODO [-46AaCfGgKkMNnqsTtVvXxYy] [-B bind_interface] [-b bind_address] [-c
			// cipher_spec] [-D [bind_address:]port] [-E log_file] [-e escape_char] [-F
			// configfile] [-I pkcs11] [-J destination] [-L address] [-m mac_spec] [-O
			// ctl_cmd] [-o option] [-Q query_option] [-R address] [-S ctl_path] [-W
			// host:port] [-w local_tun[:remote_tun]]
			String keyFile = keyTextField.getText();
			if (!"".equalsIgnoreCase(keyFile)) {
				command.add("-i");
				command.add(keyFile);
			}
			String userKnownHosts = knownHostsField.getText();
			if (!"".equalsIgnoreCase(userKnownHosts)) {
				command.add("-o");
				command.add("UserKnownHostsFile");
				command.add(userKnownHosts);
			}

			command.add(hostField.getText());
			PtyProcess proc;
			try {
				
				Map<String, String> myEnv = populateEnvironmentMap(envField.getText());
				for (String s : command) {
					SithTermMainWindow.getLogger().debug(s);
				}
				PtyProcessBuilder pb = new PtyProcessBuilder((String[]) command.toArray(new String[0]))
						.setConsole(this.getApplication().getSettings().isConsole())
						.setCygwin(this.getApplication().getSettings().isCygwin())
						.setDirectory(this.getApplication().getSettings().getDir())
						.setEnvironment(myEnv);
				
				proc = pb.start();
				SshTtyConnector conn = new SshTtyConnector(proc, StandardCharsets.UTF_8);
				jtw.setTtyConnector(conn);
				jtw.start();
			} catch (IOException e) {
				SithTermMainWindow.getLogger().error("Error starting SSH TTYConnector", e);
			}
			optsDialog.setVisible(false);

		});

		optsDialog.setVisible(true);

		return jtw;
	}

	private Map<String, String> populateEnvironmentMap(String envString) {
		Map<String, String> myEnv = new HashMap<>();
		for (Map.Entry<String, String> me : System.getenv().entrySet()) {
			myEnv.put(me.getKey(), me.getValue());
		}
		// TODO implement escaping for comma and =, as well as special characters
		
		if (!"".equalsIgnoreCase(envString)) {
			for (String s : envString.split(",")) {
				String[] tmpEnv = s.split("=");
				if (tmpEnv.length > 1)
					myEnv.put(tmpEnv[0], tmpEnv[1]);
			}
		}
		return myEnv;
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
	}

	@Override
	public String getPluginName() {
		return "SecureShellPlugin";
	}

}
