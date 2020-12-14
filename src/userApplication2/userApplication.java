/** A program designed to communicate with the Ithaki Server of the Aristotle University of Thessaloniki
 * in terms of network socket programming. 
 * 
 * It requires port-forwarding to take place before use. Request codes are granted from the page of Ithaki.
 */

package userApplication2;

import java.net.*;
import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import java.nio.*;

public class userApplication {
	
	static final int serverPort = 38000; 
    static final int clientPort = 48000; 
    static final String echoRequest = "EXXXX"; 
    static final String imageRequest = "MXXXX"; 
    static final String audioRequest = "AXXXX"; 
    static final String ithakiCopterRequest = "QXXXX";
    static final String vehicleRequest = "VXXXX";
    
    static final String engineRunTime = "1F";
    static final String intakeAirTemperature = "0F";
    static final String throttlePosition = "11";
    static final String engineRPM = "0C";
    static final String vehicleSpeed = "0D";
    static final String coolantTemperature = "05";
    
    public static final void main(String[] argv) throws LineUnavailableException, SocketException,
    											  UnknownHostException, IOException, ClassNotFoundException {
    	//Example of a complete session
    	userApplication.echoStatistics(true);
    	userApplication.echoStatistics(false);
    	userApplication.echoTemperatureStatistics();
    	userApplication.receiveImage("FIX", "imageCamFIX");
    	userApplication.receiveImage("PTZ", "imageCamPTZ");
    	userApplication.receiveAudioDPCM("Song", 999, "One");
    	userApplication.receiveAudioDPCM("Frequency", 999, "One");
    	userApplication.receiveAudioAQDPCM("Song", 999, "One");
    	userApplication.receiveAudioAQDPCM("Song", 999, "Two");
    	userApplication.ithakiCopter();
    	userApplication.vehicleOBD();
    }
    
    /**
     * Function to receive echo packets, delayed or non-delayed, depending on the given parameter.
     * @param delayStatus Declares if the echo packets will be delayed or not.
     * @throws SocketException If the socket cannot be opened, or if it is already in use.
     * @throws IOException If there is a file writing issue met.
     * @throws UnknownHostException If there is an issue with the serverPublicAddress later given.
     */
    
    static final void echoStatistics(final boolean delayStatus) throws SocketException, IOException, UnknownHostException {
        String messageReceived = " ";
        String echoMode = " ";
        String echoRequestCode = " ";
        if(delayStatus == true) {
        	System.out.println("Initializing reception of delayed echo messages.");
        	echoRequestCode = echoRequest + "\r";
        	echoMode = "Delayed";
        } else if(delayStatus == false) {
        	System.out.println("Initializing reception of non delayed echo messages.");
        	echoRequestCode = "E0000\r";
        	echoMode = "NonDelayed";
        }
        
        byte[] serverPublicAddress = {(byte) 155, (byte) 207, (byte) 18, (byte) 208};
        InetAddress hostAddress = InetAddress.getByAddress(serverPublicAddress);
        byte[] echoRequestCodeBytes = echoRequestCode.getBytes();
        DatagramSocket echoSendSocket = new DatagramSocket();
        DatagramPacket echoSendPacket = new DatagramPacket(echoRequestCodeBytes, echoRequestCodeBytes.length, hostAddress, serverPort);
        DatagramSocket echoReceiveSocket = new DatagramSocket(clientPort);
        echoReceiveSocket.setSoTimeout(3600);
        byte[] echoReceiveBuffer = new byte[2048];
        DatagramPacket echoReceivePacket = new DatagramPacket(echoReceiveBuffer, echoReceiveBuffer.length);
        ArrayList<Double> echoTimeStamps = new ArrayList<Double>();
        int echoPacketCounter = 0;
        double timeElapsed = 0;
        double averageTime = 0;
        double timeStart = 0;
        double timeEnd = 0;
        double initialTime = 0;
        double completionTime = 0;
        timeStart = System.nanoTime();
        int timeLimit = 5 * 60 * 1000;
        ArrayList<String> echoResults = new ArrayList<String>();
        while (timeEnd < timeLimit) {
            echoSendSocket.send(echoSendPacket);
            initialTime = System.nanoTime();
            echoPacketCounter++;
            for (; ;) {
                try {
                    echoReceiveSocket.receive(echoReceivePacket);
                    completionTime = (System.nanoTime() - initialTime) / 1000000;
                    messageReceived = new String(echoReceiveBuffer, 0, echoReceivePacket.getLength());
                    System.out.print(messageReceived);
                    System.out.println(" " + completionTime);
                    break;
                } catch (Exception x) {
                    x.printStackTrace();
                    break;
                }
            }
            
            timeElapsed += completionTime;
            echoTimeStamps.add(completionTime);
            echoResults.add("" + completionTime);
            timeEnd = (System.nanoTime() - timeStart) / 1000000;
        }
        
        ArrayList<String> echoStatistics = new ArrayList<String>();
        averageTime = timeElapsed / echoPacketCounter;
        echoStatistics.add("Total number of packets : " + String.valueOf((double) echoPacketCounter));
        echoStatistics.add("Total average time : " + String.valueOf(averageTime));
        echoStatistics.add("Session time :" + (timeElapsed / 60) / 1000 + " minutes\n");
        echoStatistics.add("Total time : " + (timeEnd / 60) / 1000 + " minutes\n");
        double sum = 0;
        float counter = 0;
        ArrayList<Float> counters = new ArrayList<Float>();
        for (int i = 0; i < echoTimeStamps.size(); i++) {
            int j = i;
            while ((sum < 8 * 1000) && (j < echoTimeStamps.size())) {
                sum += echoTimeStamps.get(j);
                counter++;
                j++;
            }
            
            counter = counter / 8;
            counters.add(counter);
            counter = 0;
            sum = 0;
        }
        
        BufferedWriter echoBufferedWriter = null;
        try {
            String fileDestination = "echoResults" + echoMode + ".txt";
            File file = new File(fileDestination);
            echoBufferedWriter = new BufferedWriter(new FileWriter(fileDestination, false));
            if (!file.exists()) {
                file.createNewFile();
            }
            
            for (int i = 0; i < echoResults.size(); i++) {

                echoBufferedWriter.write(String.valueOf(echoResults.get(i)));
                echoBufferedWriter.newLine();
            }
            
            echoBufferedWriter.newLine();
            
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (echoBufferedWriter != null) echoBufferedWriter.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        echoBufferedWriter = null;

        try {
            String fileDestination = "echoStatistics" + echoMode + ".txt";
            File file = new File(fileDestination);
            echoBufferedWriter = new BufferedWriter(new FileWriter(fileDestination, false));
            if (!file.exists()) {
                file.createNewFile();
            }
            
            for (int i = 0; i < echoStatistics.size(); i++) {
                echoBufferedWriter.write(String.valueOf(echoStatistics.get(i)));
                echoBufferedWriter.newLine();
            }
            
            echoBufferedWriter.newLine();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (echoBufferedWriter != null) {
                	echoBufferedWriter.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        echoBufferedWriter = null;
        try {
            String finalDestination = "echoThroughput" + echoMode + ".txt";
            File file = new File(finalDestination);
            echoBufferedWriter = new BufferedWriter(new FileWriter(finalDestination, false));
            if (!file.exists()) {
                file.createNewFile();
            }
            
            for (int i = 0; i < counters.size(); i++) {
                echoBufferedWriter.write(String.valueOf(counters.get(i)));
                echoBufferedWriter.newLine();
            }
            
            echoBufferedWriter.newLine();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (echoBufferedWriter != null) {
                	echoBufferedWriter.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        if(delayStatus == true) {
        	System.out.println("Reception of delayed echo messages complete.");
        }
        else if(delayStatus == false) {
        	System.out.println("Reception of non delayed echo messages complete.");
        }
        
        echoReceiveSocket.close();
        echoSendSocket.close();
    }
    
    /**
     * Function to collect echo packets with additional temperature information.
     * @throws SocketException If the Socket could not be opened, or if it is already in use.
     * @throws IOException If there is a file writing issue met.
     * @throws UnknownHostException If there is an issue with the serverPublicAddress later given.
     */
    
    static final void echoTemperatureStatistics() throws SocketException, IOException, UnknownHostException {
    	System.out.println("Initializing reception of temperature from collection points.");
        String echoRequestCode = " ";
        byte[] serverPublicAddress = {(byte) 155, (byte) 207, (byte) 18, (byte) 208};
        InetAddress hostAddress = InetAddress.getByAddress(serverPublicAddress);
        byte[] echoRequestCodeBytes = echoRequestCode.getBytes();
        DatagramSocket echoSendSocket = new DatagramSocket();
        DatagramPacket echoSendPacket = new DatagramPacket(echoRequestCodeBytes, echoRequestCodeBytes.length, hostAddress, serverPort);
        DatagramSocket echoReceiveSocket = new DatagramSocket(clientPort);
        echoReceiveSocket.setSoTimeout(3600);
        byte[] echoReceiveBuffer = new byte[2048];
        DatagramPacket recievePacket = new DatagramPacket(echoReceiveBuffer, echoReceiveBuffer.length);
        double initialTime = 0;
        double completionTime = 0;
        String echoMessageReceived = "";
        for (int i = 0; i <= 9; i++) {
            echoRequestCode = echoRequest + "T0" + i + "\r";
            echoRequestCodeBytes = echoRequestCode.getBytes();
            echoSendPacket = new DatagramPacket(echoRequestCodeBytes, echoRequestCodeBytes.length, hostAddress, serverPort);
            echoSendSocket.send(echoSendPacket);
            initialTime = System.nanoTime();
            for (; ;) {
                try {
                    echoReceiveSocket.receive(recievePacket);
                    completionTime = (System.nanoTime() - initialTime) / 1000000;
                    echoMessageReceived = new String(echoReceiveBuffer, 0, recievePacket.getLength());
                    System.out.println(echoMessageReceived);
                    System.out.print("" + completionTime + "\n");
                    break;
                } catch (Exception x) {
                    System.out.println(x);
                    break;
                }
            }
        }
        
        System.out.println("Reception of temperature from collection points complete.");
        echoSendSocket.close();
        echoReceiveSocket.close();
    }
    
    /**
     * Function to receive an image from the selected camera, depending on the parameter given.
     * @param camMode Indicates which camera will be used to get a picture taken and later receive it.
     * @param fileName Indicates the name that will be given to the file created.
     * @throws SocketException If the socket cannot be opened, or if it is already in use.
     * @throws IOException If there is a file writing issue met.
     * @throws UnknownHostException If there is an issue with the serverPublicAddress later given.
     */
    
	static final void receiveImage(final String camMode, final String fileName) throws SocketException, IOException, UnknownHostException {
        String imageRequestCode = " ";
        if(camMode == "FIX") {
        	System.out.println("Initializing reception of FIX mode image.");
        	imageRequestCode = imageRequest + " UDP=1024 CAM=FIX\r";
        } else if (camMode == "PTZ") {
        	System.out.println("Initializing reception of PTZ mode image.");
        	imageRequestCode = imageRequest + " UDP=1024 CAM=PTZ\r";
        } else {
        	System.out.println("Invalid 'camMode' String given, can't continue session.");
        	return;
        }
        
        byte[] serverPublicAddress = {(byte) 155, (byte) 207, (byte) 18, (byte) 208};
        InetAddress hostAddress = InetAddress.getByAddress(serverPublicAddress);
        byte[] imageRequestCodeBytes = imageRequestCode.getBytes();
        DatagramSocket imageSendSocket = new DatagramSocket();
        DatagramPacket imageSendPacket = new DatagramPacket(imageRequestCodeBytes, imageRequestCodeBytes.length, hostAddress, serverPort);
        DatagramSocket imageReceiveSocket = new DatagramSocket(clientPort);
        imageReceiveSocket.setSoTimeout(3600);
        byte[] imageReceiveBuffer = new byte[2048];
        DatagramPacket recievePacket = new DatagramPacket(imageReceiveBuffer, imageReceiveBuffer.length);
        imageSendSocket.send(imageSendPacket);
        String outputName = (fileName + ".jpeg");
        FileOutputStream imageFileOutPutStream = null;
		try {
			imageFileOutPutStream = new FileOutputStream(outputName);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		}
        
        for (; ;) {
        	
            try {
                imageReceiveSocket.receive(recievePacket);
                if (imageReceiveBuffer == null) {
                	break;
                }
                
                for (int i = 0; i <= 1023; i++) {
                    imageFileOutPutStream.write(imageReceiveBuffer[i]);
                }
                
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }
        }
        if(camMode == "FIX") {
        	System.out.println("Reception of FIX mode image complete.");
        } else if (camMode == "PTZ") {
        	System.out.println("Reception of PTZ mode image complete.");
        }
        
        imageFileOutPutStream.close();
        imageReceiveSocket.close();
        imageSendSocket.close();
    }
	
    
	/**
	 * Function to receive a song or a frequency based on DPCM compression pattern. Makes files of Subtractions and Samples in both cases.
	 * @param audioMode Indicates what kind of audio will be received. Thus it should be only given values "Song" and "Frequency".
	 * @param numberOfPackets Indicates the number of packets that can be received, with a cap of 999 packets.
	 * @param distinguishAttribute Indicates a String to be added to the file name, so that an existing file is not overridden, in case of multiple calls of the function.
	 * @throws SocketException If the socket cannot be opened, or if it is already in use.
     * @throws IOException If there is a file writing issue met.
     * @throws UnknownHostException If there is an issue with the serverPublicAddress later given.
	 * @throws LineUnavailableException If the requested line cannot be reached, or if it is already in use.
	 */
	
    static final void receiveAudioDPCM(final String audioMode, final int numberOfPackets, final String distinguishAttribute) throws SocketException, IOException, 
    																									UnknownHostException,LineUnavailableException{
        String audioRequestCode = " ";
        if(audioMode == "Song") {
        	System.out.println("Initializing reception of a DPCM song");
        	audioRequestCode = audioRequest + "F" + String.valueOf(numberOfPackets);
        } else if (audioMode == "Frequency") {
        	System.out.println("Initializing reception of a DPCM frequency");
        	audioRequestCode = audioRequest + "T" + String.valueOf(numberOfPackets);
        } else {
        	System.out.println("Invalid 'audioMode' String given, can't continue session.");
        	return;
        }
        
        if (numberOfPackets <= 0) {
        	System.out.println("Can't receive a negative or null number of packets, can't continue session.");
        	return;
        } else if (numberOfPackets > 999) {
        	System.out.println("Can't receive a sum of packets greater than 999, can't continue session.");
        	return;
        }
        
        byte[] serverPublicAddress = {(byte) 155, (byte) 207, (byte) 18, (byte) 208};
        InetAddress hostAddress = InetAddress.getByAddress(serverPublicAddress);
        byte[] audioRequestCodeBytes = audioRequestCode.getBytes();
        DatagramSocket audioSendSocket = new DatagramSocket();
        DatagramPacket audioSendPacket = new DatagramPacket(audioRequestCodeBytes, audioRequestCodeBytes.length, hostAddress, serverPort);
        DatagramSocket audioRecieveSocket = new DatagramSocket(clientPort);
        audioRecieveSocket.setSoTimeout(3600);
        byte[] audioReceiveBuffer = new byte[128];
        DatagramPacket audioReceivePacket = new DatagramPacket(audioReceiveBuffer, audioReceiveBuffer.length);
        audioSendSocket.send(audioSendPacket);
        byte[] audioReceived = new byte[256 * numberOfPackets];
        int lowNibble, highNibble;
        int sub1, sub2;
        int count = 0;
        int x1 = 0, x2 = 0;
        ArrayList<Integer> dpcmSubs = new ArrayList<Integer>();
        ArrayList<Integer> dpcmSamples = new ArrayList<Integer>();
        for(int i = 1; i < numberOfPackets; i++) {
            try {
            	if((i % 100) == 0) {
                    System.out.println("Currently " + (1000 - i) + " packets remaining.");
                }
                audioRecieveSocket.receive(audioReceivePacket);
                for (int j = 0; j <= 127; j++){
                    lowNibble = audioReceiveBuffer[j] & 15;
                    highNibble = (audioReceiveBuffer[j] & 240) >> 4;
                    sub1 = lowNibble - 8;
                    dpcmSubs.add(sub1);
                    sub1 = sub1 * 5;
                    sub2 = highNibble - 8;
                    dpcmSubs.add(sub2);
                    sub2 = sub2 * 5;
                    x1 = x2 + sub1;
                    dpcmSamples.add(x1);
                    x2 = x1 + sub2;
                    dpcmSamples.add(x2);
                    audioReceived[count] = (byte) x1;
                    count++;
                    audioReceived[count] = (byte) x2;
                    count++;
                }
            } catch (Exception exception) {
               exception.printStackTrace();
            }
        }
        
        if(audioMode == "Song") {
        	System.out.println("Reception of DPCM Song is complete.");
            AudioFormat dpcmSong = new AudioFormat(8000, 8, 1, true, false);
            SourceDataLine songPlayer = AudioSystem.getSourceDataLine(dpcmSong);
            songPlayer.open(dpcmSong, 32000);
            songPlayer.start();
            songPlayer.write(audioReceived, 0, 256 * numberOfPackets);
            songPlayer.stop();
            songPlayer.close();
            System.out.println("Song is now currently playing.");
        } else if (audioMode == "Frequency") {
        	System.out.println("Reception of DPCM Frequency is complete.");
        }
        
        BufferedWriter subsBufferedWriter = null;
        try {
            File f = new File("dpcmSubs" + audioMode + distinguishAttribute + ".txt");
            if(!f.exists()) {
                f.createNewFile();
            }
            
            FileWriter fileWriter = new FileWriter(f, false);
            subsBufferedWriter = new BufferedWriter(fileWriter);
            for(int i = 0; i < dpcmSubs.size(); i += 2) {
                subsBufferedWriter.write("" + dpcmSubs.get(i) + " " + dpcmSubs.get(i+1));
                subsBufferedWriter.newLine();
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if(subsBufferedWriter != null) {
                	subsBufferedWriter.close();
                }
            } catch(Exception ex) {
            	ex.printStackTrace();
            }
        }
        
        BufferedWriter samplesBufferedWriter = null;
        try {
            File file = new File("dpcmSamples" + audioMode + distinguishAttribute + ".txt");
            if(!file.exists()) {
                file.createNewFile();
            }
            
            FileWriter fileWriter = new FileWriter(file, false);
            samplesBufferedWriter = new BufferedWriter(fileWriter);
            for(int i = 0; i < dpcmSamples.size(); i += 2) {
                samplesBufferedWriter.write("" + dpcmSamples.get(i) + " " + dpcmSamples.get(i+1));
                samplesBufferedWriter.newLine();
            }
            
        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if(samplesBufferedWriter != null) {
                	samplesBufferedWriter.close();
                }
            } catch(Exception ex) {
            	ex.printStackTrace();
            }
        }
        audioRecieveSocket.close();
        audioSendSocket.close();
    }
	
    /**
     * Function to receive a song on AQDPCM compression pattern. AQDPCM doesn't work on Frequencies, but is still defined for a feeling of completion.
     * Makes files of Bs, Means, Subtractions and Samples in both cases.
     * @param audioMode Indicates what kind of audio will be received. Thus it should be only given values "Song" and "Frequency".
	 * @param numberOfPackets Indicates the number of packets that can be received, with a cap of 999 packets.
	 * @param distinguishAttribute Indicates a String to be added to the file name, so that an existing file is not overridden, in case of multiple calls of the function
	 * @throws SocketException If the socket cannot be opened, or if it is already in use.
     * @throws IOException If there is a file writing issue met.
     * @throws UnknownHostException If there is an issue with the serverPublicAddress later given.
	 * @throws LineUnavailableException If the requested line cannot be reached, or if it is already in use.
     */
    
    static final void receiveAudioAQDPCM(final String audioMode, final int numberOfPackets, final String distinguishAttribute) throws SocketException, IOException, 
    																		   														UnknownHostException,LineUnavailableException{
        String audioRequestCode = " ";
        if(audioMode == "Song") {
        	System.out.println("Initializing reception of AQDPCM Song.");
        	audioRequestCode = audioRequest + "AQF" + String.valueOf(numberOfPackets);
        } else if(audioMode == "Frequency") {
        	System.out.println("Initializing reception of AQDPCM Frequency.");
        	audioRequestCode = audioRequest + "AQT" + String.valueOf(numberOfPackets);
        } else {
        	System.out.println("Invalid 'audioMode' String given, can't continue session.");
        	return;
        }
        
        if (numberOfPackets <= 0) {
        	System.out.println("Can't receive a negative or null number of packets, can't continue session.");
        	return;
        } else if (numberOfPackets > 999) {
        	System.out.println("Can't receive a sum of packets greater than 999, can't continue session.");
        	return;
        }
        
        byte[] serverPublicAddress = {(byte)155, (byte) 207, (byte) 18, (byte) 208};
        InetAddress hostAddress = InetAddress.getByAddress(serverPublicAddress);
        byte[] audioRequestCodeBytes = audioRequestCode.getBytes();
        DatagramSocket aduioSendSocket = new DatagramSocket();
        DatagramPacket audioSendPacket = new DatagramPacket(audioRequestCodeBytes, audioRequestCodeBytes.length, hostAddress, serverPort);
        DatagramSocket audioReceiveSocket = new DatagramSocket(clientPort);
        byte[] audioReceiveBuffer = new byte[132];
        DatagramPacket audioReceivePacket = new DatagramPacket(audioReceiveBuffer, audioReceiveBuffer.length);
        audioReceiveSocket.setSoTimeout(5000);
        aduioSendSocket.send(audioSendPacket);
        byte[] meanBytes = new byte[4];
        byte[] bBytes = new byte[4];
        byte sign;
        int lowNibble, highNibble;
        int sub1, sub2;
        int position = 4;
        int x1 = 0, x2 = 0;
        int mean, b, temporary = 0;
        byte[] audioReceived = new byte[256*2*numberOfPackets];
        ArrayList<Integer> aqdpcmSubs = new ArrayList<Integer>();
        ArrayList<Integer> aqdpcmSamples = new ArrayList<Integer>();
        ArrayList<Integer> aqdpcmMeans = new ArrayList<Integer>();
        ArrayList<Integer> aqdpcmBs = new ArrayList<Integer>();
        for(int i = 1; i < numberOfPackets; i++) {
            try {
            	if(( i % 100) == 0) {
                    System.out.println("Currently " + (1000 - i) + " packets remaining.");
                }
                audioReceiveSocket.receive(audioReceivePacket);
                sign = (byte)( (audioReceiveBuffer[1] & 0x80) !=0 ? 0xff : 0x00);
                meanBytes[3] = sign;
                meanBytes[2] = sign;
                meanBytes[1] = audioReceiveBuffer[1];
                meanBytes[0] = audioReceiveBuffer[0];
                mean = ByteBuffer.wrap(meanBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
                aqdpcmMeans.add(mean);
                sign = (byte)( (audioReceiveBuffer[3] & 0x80) !=0 ? 0xff : 0x00);
                bBytes[3] = sign;
                bBytes[2] = sign;
                bBytes[1] = audioReceiveBuffer[3];
                bBytes[0] = audioReceiveBuffer[2];
                b = ByteBuffer.wrap(bBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
                aqdpcmBs.add(b);

                for (int j = 4;j <= 131;j++) {
                    lowNibble = (int) (audioReceiveBuffer[j] & 0x0000000F);
                    highNibble = (int) ((audioReceiveBuffer[j] & 0x000000F0) >> 4);
                    sub1 = (highNibble - 8);
                    aqdpcmSubs.add(sub1);
                    sub2 = (lowNibble - 8);
                    aqdpcmSubs.add(sub2);
                    sub1 = sub1 * b;
                    sub2 = sub2 * b;
                    x1 = temporary + sub1 + mean;
                    aqdpcmSamples.add(x1);
                    x2 = sub1 + sub2 + mean;
                    temporary = sub2;
                    aqdpcmSamples.add(x2);
                    position += 4;
                    audioReceived[position] = (byte) (x1 & 0x000000FF);
                    audioReceived[position + 1] = (byte) ((x1 & 0x0000FF00) >> 8);
                    audioReceived[position + 2] = (byte) (x2 & 0x000000FF);
                    audioReceived[position + 3] = (byte) ((x2 & 0x0000FF00) >> 8);
                }
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
        
        if(audioMode == "Song") {
        	System.out.println("Reception of AQDPCM Song is complete.");
            AudioFormat aqpcmSong = new AudioFormat(8000, 16, 1, true, false);
            SourceDataLine songPlayer = AudioSystem.getSourceDataLine(aqpcmSong);
            songPlayer.open(aqpcmSong, 32000);
            songPlayer.start();
            songPlayer.write(audioReceived, 0, 256 * 2 * numberOfPackets);
            songPlayer.stop();
            songPlayer.close();
            System.out.println("Song is now currently playing.");
        }
        
        BufferedWriter subsBufferedWriter = null;
        try {
            File file = new File("aqdpcmSubs" + audioMode + distinguishAttribute + ".txt");
            if(!file.exists()) {
                file.createNewFile();
            }
            
            FileWriter fw = new FileWriter(file, false);
            subsBufferedWriter = new BufferedWriter(fw);
            for(int i = 0; i < aqdpcmSubs.size(); i += 2) {
                subsBufferedWriter.write("" + aqdpcmSubs.get(i) + " " + aqdpcmSubs.get(i+1));
                subsBufferedWriter.newLine();
            }

        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if(subsBufferedWriter != null) {
                	subsBufferedWriter.close();
                }
            } catch(Exception ex) {
            	ex.printStackTrace();
            }
        }
        
        BufferedWriter samplesBufferedWriter = null;
        try {
            File file = new File("aqdpcmSamples" + audioMode + distinguishAttribute + ".txt");
            if(!file.exists()){
                file.createNewFile();
            }
            
            FileWriter fw = new FileWriter(file, false);
            samplesBufferedWriter = new BufferedWriter(fw);
            for(int i = 0; i < aqdpcmSamples.size(); i += 2) {
                samplesBufferedWriter.write("" + aqdpcmSamples.get(i) + " " + aqdpcmSamples.get(i+1));
                samplesBufferedWriter.newLine();
            }

        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if(samplesBufferedWriter != null) {
                	samplesBufferedWriter.close();
                }
            } catch(Exception ex) {
            	ex.printStackTrace();
            }
        }
        
        BufferedWriter meansBufferedWriter = null;
        try {
            File file = new File("aqdpcmMeans" + audioMode + distinguishAttribute + ".txt");
            if(!file.exists()){
                file.createNewFile();
            }
            
            FileWriter fw = new FileWriter(file, false);
            meansBufferedWriter = new BufferedWriter(fw);
            for(int i = 0 ; i < aqdpcmMeans.size() ; i += 2) {
                meansBufferedWriter.write("" + aqdpcmMeans.get(i));
                meansBufferedWriter.newLine();
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if(meansBufferedWriter != null) {
                	meansBufferedWriter.close();
                }
            } catch(Exception ex) {
            	ex.printStackTrace();
            }
        }
        
        BufferedWriter bBufferedWriter = null;
        try {
            File file = new File("aqdpcmBs" + audioMode + distinguishAttribute + ".txt");
            if(!file.exists()){
                file.createNewFile();
            }
            
            FileWriter fw = new FileWriter(file,false);
            bBufferedWriter = new BufferedWriter(fw);
            for(int i = 0 ; i < aqdpcmBs.size() ; i ++) {
                bBufferedWriter.write("" + aqdpcmBs.get(i));
                bBufferedWriter.newLine();
            }

        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if(bBufferedWriter != null) bBufferedWriter.close();
            } catch(Exception ex){
            	ex.printStackTrace();
            }
        }
        
        audioReceiveSocket.close();
        aduioSendSocket.close();
    }
    
    /**
     * Function to receive statistics taken from the IthakiCopter platform. Requires ithakimodem.jar open and in use in order to work properly.
     * Creates a file, saving the previously stated statistics there.
     * @throws SocketException If the socket cannot be opened, or if it is already in use.
     * @throws IOException If there is a file writing issue met.
     * @throws UnknownHostException If there is an issue with the serverPublicAddress later given.
	 * @throws LineUnavailableException If the requested line cannot be reached, or if it is already in use.
     * @throws ClassNotFoundException - If class named throughout the function, cannot be found.
     */
    
    static final void ithakiCopter() throws SocketException, IOException, UnknownHostException, 
    												   LineUnavailableException,ClassNotFoundException {
    	System.out.println("Initializing Ithaki Copter Telemetry.");
    	String messageReceived = " ";
		int leftMotor,rightMotor;
		DatagramSocket ithakiCopterReceiveSocket = new DatagramSocket(48078);
		ithakiCopterReceiveSocket.setSoTimeout(5000);
		byte[] ithakiCopterReceiveBuffer = new byte[113];
		DatagramPacket ithakiCopterReceivePacket = new DatagramPacket(ithakiCopterReceiveBuffer, ithakiCopterReceiveBuffer.length);
		File ithakiCopterStats = new File("ithakiCopter.csv");
		BufferedWriter ithakiCopterBufferedWriter = null;
		try {
			ithakiCopterBufferedWriter = new BufferedWriter(new FileWriter(ithakiCopterStats, true));
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		StringBuilder ithakiCopterStringBuilder = new StringBuilder();
		for( ; ; ) {
			try {
				ithakiCopterReceiveSocket.receive(ithakiCopterReceivePacket);	
			} catch(Exception x) {
	        	x.printStackTrace();
	        	break;
			}
	        messageReceived = new String(ithakiCopterReceiveBuffer, 0, ithakiCopterReceiveBuffer.length);
	        System.out.println(messageReceived);
	        leftMotor = Integer.parseInt(messageReceived.substring(40, 43));
	        rightMotor = Integer.parseInt(messageReceived.substring(51, 54));
	        if(leftMotor == 0 && rightMotor == 0) {
	        	System.out.println("Both motors have stopped, telemetry is complete.");
	        	break;
	        }
	        
	        ithakiCopterStringBuilder.append(messageReceived.substring(64, 67) + ", ");
	        ithakiCopterStringBuilder.append(messageReceived.substring(80, 86) + ", ");
	        ithakiCopterStringBuilder.append(messageReceived.substring(96, 103) + "\n");        
		}
		
		ithakiCopterBufferedWriter.write(ithakiCopterStringBuilder.toString());
		ithakiCopterBufferedWriter.close();
		ithakiCopterReceiveSocket.close();
    	}
    
    /**
     * Function to receive statistics from the 'vehicle OBD - II'. It creates a file, with the statistics mentioned, after they have been processed,
     * as they are received in a Hexademical 'type of encoding'.
     * @throws SocketException If the socket cannot be opened, or if it is already in use.
     * @throws IOException If there is a file writing issue met.
     * @throws UnknownHostException If there is an issue with the serverPublicAddress later given.
	 * @throws LineUnavailableException If the requested line cannot be reached, or if it is already in use.
     * @throws ClassNotFoundException - If class named through the function, cannot be found.
     */
    
    static void vehicleOBD() throws SocketException, IOException, UnknownHostException,
    													   LineUnavailableException, ClassNotFoundException {
    	System.out.println("Initializing reception of Vehicle OBD - II statistics.");
        String vehicleRequestCode = " ";
        String messageReceived = " ";
        StringBuilder vehicleStringBuilder = new StringBuilder();
        double beginLoop = 0;
        double finishloop = 0;
        byte[] serverPublicAddress = {(byte)155, (byte)207, (byte) 18, (byte)208};
        InetAddress hostAddress = InetAddress.getByAddress(serverPublicAddress);
        DatagramSocket vehicleSendSocket = new DatagramSocket();
        DatagramSocket vehicleReceiveSocket = new DatagramSocket(clientPort);
        byte[] vehicleReceiveBuffer = new byte[5000];
        DatagramPacket vehicleReceivePacket = new DatagramPacket(vehicleReceiveBuffer, vehicleReceiveBuffer.length);
        vehicleReceiveSocket.setSoTimeout(5000);
        beginLoop = System.nanoTime();
        while(finishloop < 4 * 60 * 1000) {
        	String[] pidArray = {engineRunTime, intakeAirTemperature, throttlePosition, engineRPM, vehicleSpeed, coolantTemperature};
        	for(String i: pidArray) {
            vehicleRequestCode = vehicleRequest +"OBD=01 " + i +"\r";
            byte[] vehicleRequestCodeBytes = vehicleRequestCode.getBytes();
            DatagramPacket vehicleSendPacket = new DatagramPacket(vehicleRequestCodeBytes, vehicleRequestCodeBytes.length, hostAddress,serverPort);
            try {
            	String xx = "";
            	String yy = "";
            	int xxd = 0;
            	int yyd = 0;
            	int vehicleMeasurement = 0;
                vehicleSendSocket.send(vehicleSendPacket);
                vehicleReceiveSocket.receive(vehicleReceivePacket);
                messageReceived = new String(vehicleReceiveBuffer, 0, vehicleReceivePacket.getLength());
                switch (i) {
				case engineRunTime: {
					xx = messageReceived.substring(6, 8);
					yy = messageReceived.substring(9, 11);
					xxd = Integer.parseInt(xx, 16);
					yyd = Integer.parseInt(yy, 16);
					vehicleMeasurement = 256 * xxd + yyd;
					break;
				}
				case intakeAirTemperature: {
					xx = messageReceived.substring(6, 8);
					xxd = Integer.parseInt(xx, 16);
					vehicleMeasurement = xxd - 40;
					break;
				}
				case throttlePosition: {
					xx = messageReceived.substring(6, 8);
					xxd = Integer.parseInt(xx, 16);
					vehicleMeasurement = xxd * 100 / 255;
					break;
				}
				case engineRPM: {
					xx = messageReceived.substring(6, 8);
					yy = messageReceived.substring(9, 11);
					xxd = Integer.parseInt(xx, 16);
					yyd = Integer.parseInt(yy, 16);
					vehicleMeasurement = ((xxd * 256) + yyd) / 4;
					break;
				}
				case vehicleSpeed: {
					xx = messageReceived.substring(6, 8);
					xxd = Integer.parseInt(xx, 16);
					vehicleMeasurement = xxd;
					break;
				}
				case coolantTemperature: {
					xx = messageReceived.substring(6, 8);
					xxd = Integer.parseInt(xx, 16);
					vehicleMeasurement = xxd - 40;
					break;
				}

				}
                System.out.print(vehicleMeasurement + ", ");
                vehicleStringBuilder.append(vehicleMeasurement + ", ");
            } catch(Exception ex) {
            	ex.printStackTrace();
            }
        }
        	vehicleStringBuilder.append("\n");
        	System.out.println();
        	finishloop = (System.nanoTime() - beginLoop) / 1000000;
        }
        BufferedWriter vehicleBufferedWriter = null;
        try {
            File file = new File("vehicle.csv");
            if(!file.exists()) {
                file.createNewFile();
            }
            
            FileWriter fileWriter = new FileWriter(file, true);
            vehicleBufferedWriter = new BufferedWriter(fileWriter);
            vehicleBufferedWriter.write(vehicleStringBuilder.toString());

        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if(vehicleBufferedWriter != null) {
                	vehicleBufferedWriter.close();
                }
            } catch(Exception ex) {
            	ex.printStackTrace();
            }
        }
        
        System.out.println("Reception of Vehicle OBD II statistics is complete.");
        vehicleReceiveSocket.close();
        vehicleSendSocket.close();
    }
    
}