package kmeans;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import kmeansExcept.ServerException;

@SuppressWarnings("serial")
public class KMeans extends JFrame {
	private ObjectOutputStream outObject;
	private ObjectInputStream inObject;

	class TabbelPane extends JPanel {
		private JPanelCluster panelDB;
		private JPanelCluster panelFile;

		private TabbelPane() {
			panelDB = new JPanelCluster("MINE", action -> {
				try {
					learningFromDBAction();
				} catch (ClassNotFoundException e) {
					JOptionPane.showMessageDialog(this, e.getMessage() + "ciao1", "Error", JOptionPane.ERROR_MESSAGE);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(this, e.getMessage() + "ciao11", "Error", JOptionPane.ERROR_MESSAGE);

				} catch (ServerException e) {
					JOptionPane.showMessageDialog(this, e.getMessage() + "ciao2", "Error", JOptionPane.ERROR_MESSAGE);
				}
			});
			

			panelFile = new JPanelCluster("STORE FROM FILE", action -> {
				try {
					learningFromFileAction();
				} catch (ClassNotFoundException | IOException e) {
					JOptionPane.showMessageDialog(this, e.getMessage() + "ciao3", "Error", JOptionPane.ERROR_MESSAGE);
				}
			});
			

			this.add(panelFile);
			this.add(panelDB);
		}

		private void learningFromDBAction()
				throws SocketException, ServerException, IOException, ClassNotFoundException {
			String result = "";

			int kValue;
			try {

				kValue = new Integer(panelDB.kText.getText()).intValue();

			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			String tableName = panelDB.tableText.getText();

			if (kValue < 1) {
				JOptionPane.showMessageDialog(this, "Insert an a positiv integer", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			outObject.writeObject(0);
			outObject.writeObject(tableName);

			result = (String) inObject.readObject();

			if (!result.equals("OK")) {
				JOptionPane.showMessageDialog(this, result, "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			outObject.writeObject(1);
			outObject.writeObject(kValue);
			result = (String) inObject.readObject();

			if (!result.equals("OK")) {
				JOptionPane.showMessageDialog(this, result, "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			result = (String) inObject.readObject(); // numero di iterazioni
			result += "\n" + inObject.readObject(); // kmeans output
			panelDB.clusterOutput.setText(result);
			outObject.writeObject(2);
			String toSend = tableName + kValue + ".dat";
			outObject.writeObject(toSend);
			result = (String) inObject.readObject();

			if (!result.equals("OK")) {
				JOptionPane.showMessageDialog(this, result, "Error", JOptionPane.ERROR_MESSAGE);
				return;
			} else {
				JOptionPane.showMessageDialog(this, "Operation successfully completed", "Done",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}

		private void learningFromFileAction() throws SocketException, IOException, ClassNotFoundException {

			String tableName = panelFile.tableText.getText();
			int kValue;
			try {

				kValue = new Integer(panelFile.kText.getText()).intValue();
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			outObject.writeObject(3);
			outObject.writeObject(tableName);
			String result = (String) inObject.readObject();
			if(!result.equals("OK")) {
				JOptionPane.showMessageDialog(this, result, "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			outObject.writeObject(kValue);

			result = (String) inObject.readObject();

			if (!result.equals("OK")) {
				JOptionPane.showMessageDialog(this, result, " Server Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			JOptionPane.showMessageDialog(this, "Operation successfully completed");
			result = (String) inObject.readObject();
			panelFile.clusterOutput.setText(result);

		}

		class JPanelCluster extends JPanel {
			private JTextField tableText = new JTextField(20);
			private JTextField kText = new JTextField(10);
			private JTextArea clusterOutput = new JTextArea(20, 45);
			private JButton executeButton;

			public JPanelCluster(String buttonName, ActionListener actionListener) {
				super(new GridLayout(3, 1));
				this.setLayout(new BorderLayout());
				JPanel northPanel = new JPanel(new FlowLayout());
				JLabel tableLAbel = new JLabel("Table Name");

				northPanel.add(tableText, FlowLayout.LEFT);
				northPanel.add(tableLAbel, FlowLayout.LEFT);
				northPanel.add(kText, FlowLayout.RIGHT);
				northPanel.add(new JLabel("K"), FlowLayout.RIGHT);

				JPanel centralPanel = new JPanel();
				JScrollPane scroll = new JScrollPane(clusterOutput);
				clusterOutput.setEditable(false);
				centralPanel.add(scroll);

				JPanel southPanel = new JPanel();
				executeButton = new JButton(buttonName);
				executeButton.addActionListener(actionListener);
				southPanel.add(executeButton);

				this.add(northPanel, BorderLayout.NORTH);
				this.add(centralPanel, BorderLayout.CENTER);
				this.add(southPanel, BorderLayout.SOUTH);
			}

		}
	}

	public void init(String host, int port) throws IOException {
		Container contentPan = getContentPane();
		contentPan.setLayout(new GridLayout(1, 1));

		TabbelPane tabbedPane = new TabbelPane();
		JTabbedPane tab = new JTabbedPane();
		tab.add("DB", tabbedPane.panelDB);
		tab.add("File", tabbedPane.panelFile);

		contentPan.add(tab);

		// Comunication with server
		InetAddress addr = InetAddress.getByName(host); // ip
		final Socket socket = new Socket(addr, port); // Port;

		outObject = new ObjectOutputStream(socket.getOutputStream());
		inObject = new ObjectInputStream(socket.getInputStream());
		/// stream con richieste del client

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				super.windowClosing(event);
				if (inObject != null)
					try {
						inObject.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

				if (outObject != null)
					try {
						outObject.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

				try {
					socket.close();
				} catch (IOException e) {

				}
			}
		});

		////

		setSize(600, 400);
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	public static void main(String[] args) {
		KMeans km = new KMeans();
		try {
			km.init("localhost", 8080);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}