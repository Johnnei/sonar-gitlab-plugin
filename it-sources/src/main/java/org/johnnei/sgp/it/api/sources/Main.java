package org.johnnei.sgp.it.api.sources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by Johnnei on 2016-12-04.
 */
public class Main {

	private boolean myState;

	public void processFile() {
		try {
			FileInputStream inputStream = new FileInputStream(new File("myfile.txt"));
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read file.");
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public void refreshObject() {
		try {
			int a = 4;
			switch (a) {
				case 1:
					if (myState) {
						myState = false;
						if (a > 5) {
							myState = true;
						}
						return;
					} else {
						myState = true;
					}
					break;
			}
		} finally {
			myState = true;
		}
	}
}
