package br.com.uppersystems.scp;

import com.jcraft.jsch.*;
import java.io.*;

public class ScpTo {
	public static void main(String[] arg) {

		FileInputStream fis = null;
		try {

			String localPathnameFile = "/Users/filipegermano/Downloads/testescpjava.txt";
			String nfsUser = "ubuntu";
			String nfsHost = "ec2-18-224-229-88.us-east-2.compute.amazonaws.com";
			String remotePathnameFile = "/tmp";
			//usar, hoje não é criado se o arquivo estiver vazio
			Boolean createEmptyFile = false;

			JSch jsch = new JSch();
			jsch.addIdentity("/Users/filipegermano/Downloads/upper.pem");
			jsch.setConfig("StrictHostKeyChecking", "no");
			
			Session session = jsch.getSession(nfsUser, nfsHost, 22);
			session.connect();
			
			// exec 'scp -t rfile' remotely
			remotePathnameFile = remotePathnameFile.replace("'", "'\"'\"'");
			remotePathnameFile = "'" + remotePathnameFile + "'";
			String command = "scp " + " -t " + remotePathnameFile;
			
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0) {
				System.exit(0);
			}

			File _lfile = new File(localPathnameFile);

			// send "C0644 filesize filename", where filename should not include '/'
			long filesize = _lfile.length();
			command = "C0644 " + filesize + " ";
			if (localPathnameFile.lastIndexOf('/') > 0) {
				command += localPathnameFile.substring(localPathnameFile.lastIndexOf('/') + 1);
			} else {
				command += localPathnameFile;
			}
			command += "\n";
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				System.exit(0);
			}

			// send a content of lfile
			fis = new FileInputStream(localPathnameFile);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len); // out.flush();
			}
			fis.close();
			fis = null;
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0) {
				System.exit(0);
			}
			out.close();

			channel.disconnect();
			session.disconnect();

			System.exit(0);
		} catch (Exception e) {
			System.out.println(e);
			try {
				if (fis != null)
					fis.close();
			} catch (Exception ee) {
			}
		}
	}

	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}

}
