package org.n52.wps.extension;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.algorithm.annotation.LiteralDataOutput;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;

@Algorithm(version = "1.0.0")
public class GetFuelPriceProcess extends AbstractAnnotatedAlgorithm {

	private static Logger LOGGER = Logger.getLogger(GetFuelPriceProcess.class);
	private static ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private String fuelType;
	private double fuelPrice = 0.0;
	private File fuelPriceStorage = new File(
			System.getProperty("java.io.tmpdir") + File.separator
					+ "wpsFuelPriceStorage.txt");

	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
			"dd.MM.yyyy HH:mm");

	@LiteralDataOutput(identifier = "fuelPrice")
	public double getFuelPrice() {
		return fuelPrice;
	}

	@LiteralDataInput(identifier = "fuelType", minOccurs = 1)
	public void setFuelType(String fuelType) {
		this.fuelType = fuelType;
	}

	@Execute
	public void executeGetFuelPrice() {

		if (fuelPriceStorage.exists()) {

			try {

				String content = "";
				String line = "";
				String lastLine = "";
				readWriteLock.readLock().lock();

				try {

					BufferedReader bufferedReader = new BufferedReader(
							new FileReader(fuelPriceStorage));

					while ((line = bufferedReader.readLine()) != null) {
						content = content.concat(line + "\n");
						lastLine = line;
					}

				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
					return;
				} finally {
					readWriteLock.readLock().unlock();
				}

				String[] fuelPricesArray = lastLine.split(";");

				fuelPrice = getFuelPriceFromArray(fuelPricesArray);

				String dateTime = fuelPricesArray[0] + " " + fuelPricesArray[1];

				Date fuelPriceDate = simpleDateFormat.parse(dateTime);

				long timeDifference = (new Date().getTime() - fuelPriceDate
						.getTime());

				if (timeDifference > 10 * 60 * 1000) {
					
					LOGGER.info("Last fuel price from storage older than ten minutes.");
					
					String newFuelPrices = getLatestFuelPrice();

					content = content.concat(newFuelPrices);

					writeFuelPriceToStorage(content);
				}

			} catch (Exception e) {
				LOGGER.warn(e.getMessage(), e);
				return;
			}
		} else {

			try {
				
				LOGGER.info("No fuel prices in storage, requesting new ones.");
				
				String content = getLatestFuelPrice();

				String[] fuelPricesArray = content.split(";");

				fuelPrice = getFuelPriceFromArray(fuelPricesArray);

				writeFuelPriceToStorage(content);

			} catch (Exception e) {
				LOGGER.warn(e.getMessage(), e);
			}

		}

	}

	private void writeFuelPriceToStorage(String content) {

		LOGGER.info("Writing new fuel prices to storage");

		readWriteLock.writeLock().lock();

		LOGGER.info("Write locked.");

		try {

			BufferedWriter writer = new BufferedWriter(new FileWriter(
					fuelPriceStorage));

			writer.write(content);

			writer.close();

		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			readWriteLock.writeLock().unlock();

			LOGGER.info("Write unlocked.");
		}
	}

	private double getFuelPriceFromArray(String[] fuelPricesArray) {
		// for positions of additional fuel types check
		// http://export.benzinpreis-aktuell.de/exportskript
		if (fuelType.equals("gasoline")) {
			return Double.parseDouble(fuelPricesArray[2]);
		} else if (fuelType.equals("diesel")) {
			return Double.parseDouble(fuelPricesArray[5]);
		}
		return 0.0;
	}

	private String getLatestFuelPrice() throws Exception {

		LOGGER.info("Requesting latest fuel price");

		// the API-key (code) was generated for envirocar.org
		URL url = new URL(
				"http://export.benzinpreis-aktuell.de/exportdata.txt?code=69T36ft7QDY70L4");

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				url.openStream()));

		String content = "";
		String line = "";

		while ((line = reader.readLine()) != null) {
			content = content.concat(line);
		}

		LOGGER.info("Done: " + content);

		return content;

	}

}
