import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class Main {
	private static int itrNum = 50;

	public static double run(double ratio) throws GRBException {
		// V_ipt: Production cost of a unit of product i by producer p in period
		// t
		double[][][] v = { { { 50, 45 }, { 46, 42 } },
				{ { 48, 50 }, { 42, 40 } } };

		// V'_ipt: Production cost of a unit of product i by producer p in
		// period t in overtime
		double[][][] v1 = { { { 80, 90 }, { 65, 70 } },
				{ { 75, 85 }, { 50, 70 } } };

		// H_it: Holding cost of a unit of product i in central warehouse in
		// period t
		double[][] h0 = { { 10, 12 }, { 14, 16 } };

		// H'_idt: Holding cost of a unit of product i in distribution center d
		// in period t
		double[][][] h1 = { { { 8, 12 }, { 6, 10 } }, { { 6, 10 }, { 11, 15 } } };

		// E_ipt: Transportation cost of a unit of product i from producer p to
		// central warehouse in period t
		double[][][] e0 = { { { 9, 9 }, { 14, 14 } },
				{ { 9, 9 }, { 14, 14} } };

		// E'_idht: Transportation cost of a unit of product i from DC d to
		// customer h in period t
		double[][][][] e1 = {
				{ { { 10, 12 }, { 12, 14 } }, { { 9, 11 }, { 11, 13 } } },
				{ { { 8, 11 }, { 10, 14 } }, { { 7, 10 }, { 9, 13 } } } };

		// E"_idd't: Transportation cost of a unit of product i from DC d to DC
		// d' in period t
		double[][][][] e2 = {
				{ { { 0, 0 }, { 4, 10 } }, { { 6, 8 }, { 0, 0 } } },
				{ { { 0, 0 }, { 3, 6 } }, { { 9, 12 }, { 0, 0 } } } };

		// E"'_idt: Transportation cost of a unit of product i from warehouse to
		// DC d in period t
		double[][][] e3 = { { { 8, 40 }, { 11, 25 } },
				{ { 7, 30 }, { 10, 35 } } };

		// B_iht: Backorder cost of a unit of product i
		// lost from the demand of customer h in period t
		double[][][] b = { { { 135, 160 }, { 150, 160 } },
				{ { 130, 165 }, { 155, 155 } } };

		// L_iht: Demand of customer h for product i in period t
		double[][][] l = { { { 200, 140 }, { 95, 187 } },
				{ { 160, 190 }, { 210, 168 } } };

		// K_p: Capacity of producer p
		double[] k = { 2500, 2900 };

		// K1_p: Overtime capacity of producer p
		double[] k1 = { 1000, 1200 };

		// F: Capacity of central warehouse
		double f0 = 7000;

		// F1_d: Capacity of DC d
		double[] f1 = { 3000, 2500 };

		GRBEnv env = new GRBEnv("RpHND.log");
		GRBModel model = new GRBModel(env);
		model.getEnv().set(GRB.IntParam.OutputFlag, 0);

		int I = e1.length;
		int P = e0[0].length;
		int D = e1[0].length;
		int H = e1[0][0].length;
		int T = e1[0][0][0].length;

		// q_idt: Amount of product i delivered from
		// central warehouse to DC d in period t
		GRBVar[][][] q0 = new GRBVar[I][D][T];
		for (int i = 0; i < I; i++) {
			for (int d = 0; d < D; d++) {
				for (int t = 0; t < T; t++) {
					q0[i][d][t] = model.addVar(0, GRB.INFINITY, e3[i][d][t],
							GRB.INTEGER, "q0-" + i + "_" + d + "_" + t);
				}
			}
		}

		// q'_idht: Amount of product i delivered from
		// DC d to
		// customer h in period t
		GRBVar[][][][] q1 = new GRBVar[I][D][H][T];
		for (int i = 0; i < I; i++) {
			for (int d = 0; d < D; d++) {
				for (int h = 0; h < H; h++) {
					for (int t = 0; t < T; t++) {
						q1[i][d][h][t] = model.addVar(0, GRB.INFINITY,
								e1[i][d][h][t], GRB.INTEGER, "q1-" + i + "_" + d
										+ "_" + h + "_" + t);

					}
				}
			}
		}

		// q"_iddt: : Amount of product i transported
		// from DC d to DC d' in period t
		GRBVar[][][][] q2 = new GRBVar[I][D][D][T];
		for (int i = 0; i < I; i++) {
			for (int d = 0; d < D; d++) {
				for (int _d = 0; _d < D; _d++) {
					for (int t = 0; t < T; t++) {
						q2[i][d][_d][t] = model.addVar(0, GRB.INFINITY,
								e2[i][d][_d][t], GRB.INTEGER, "q2-" + i + "_"
										+ d + "_" + _d + "_" + t);

					}
				}
			}
		}

		// Q_ipt: Amount of product i produced by producer p in period t
		GRBVar[][][] Q0 = new GRBVar[I][P][T];
		for (int i = 0; i < I; i++) {
			for (int p = 0; p < P; p++) {
				for (int t = 0; t < T; t++) {
					Q0[i][p][t] = model.addVar(0, GRB.INFINITY, e0[i][p][t]
							+ v[i][p][t], GRB.INTEGER, "Q" + i + "_" + p + "_"
							+ t);
				}
			}
		}

		// Q'_ipt: Amount of product i produced by producer p in period t in
		// overtime
		GRBVar[][][] Q1 = new GRBVar[I][P][T];
		for (int i = 0; i < I; i++) {
			for (int p = 0; p < P; p++) {
				for (int t = 0; t < T; t++) {
					Q1[i][p][t] = model.addVar(0, GRB.INFINITY, e0[i][p][t]
							+ v1[i][p][t], GRB.INTEGER, "Q'" + i + "_" + p
							+ "_" + t);
				}
			}
		}

		// I_it: Inventory on hand of product i produced in
		// period t at central warehouse
		GRBVar[][] I0 = new GRBVar[I][T];
		for (int i = 0; i < I; i++) {
			for (int t = 0; t < T; t++) {
				I0[i][t] = model.addVar(0, GRB.INFINITY, h0[i][t], GRB.INTEGER,
						"I" + i + "_" + t);

			}
		}

		// I'_idt: : Inventory on hand of product i in
		// period t at DC d
		GRBVar[][][] I1 = new GRBVar[I][D][T];
		for (int i = 0; i < I; i++) {
			for (int d = 0; d < D; d++) {
				for (int t = 0; t < T; t++) {
					I1[i][d][t] = model.addVar(0, GRB.INFINITY, h1[i][d][t],
							GRB.INTEGER, "I'" + i + "_" + d + "_" + t);

				}
			}
		}

		// B_iht: : Backorder of product i for customer
		// h in period t
		GRBVar[][][] B = new GRBVar[I][H][T];
		for (int i = 0; i < I; i++) {
			for (int h = 0; h < H; h++) {
				for (int t = 0; t < T; t++) {
					B[i][h][t] = model.addVar(0, GRB.INFINITY, b[i][h][t]
							/*ratio*v[i][0][t]*/,GRB.INTEGER, "B" + i + "_" + h + "_" + t);
				}
			}
		}

		// Update model
		model.update();

		// Constraint 2
		GRBLinExpr expr;
		for (int p = 0; p < P; p++) {
			for (int t = 0; t < T; t++) {

				expr = new GRBLinExpr();
				for (int i = 0; i < I; i++) {
					expr.addTerm(1, Q0[i][p][t]);
				}
				model.addConstr(expr, GRB.LESS_EQUAL, k[p], null);

			}
		}

		// Constraint 3

		for (int p = 0; p < P; p++) {
			for (int t = 0; t < T; t++) {

				expr = new GRBLinExpr();
				for (int i = 0; i < I; i++) {
					expr.addTerm(1, Q1[i][p][t]);
				}
				model.addConstr(expr, GRB.LESS_EQUAL, k1[p], null);

			}
		}

		// Constraint 4

		for (int t = 0; t < T; t++) {

			expr = new GRBLinExpr();

			for (int i = 0; i < I; i++) {
				if (t > 0)
					expr.addTerm(1, I0[i][t]);
				for (int p = 0; p < P; p++) {
					expr.addTerm(1, Q0[i][p][t]);
					expr.addTerm(1, Q1[i][p][t]);
				}
			}
			model.addConstr(expr, GRB.LESS_EQUAL, f0, null);

		}

		// Constraint 5 > 6

		for (int t = 0; t < T; t++) {
			for (int d = 0; d < D; d++) {

				expr = new GRBLinExpr();
				for (int i = 0; i < I; i++) {
					if (t > 0)
						expr.addTerm(1, I1[i][d][t - 1]);
					expr.addTerm(1, q0[i][d][t]);
					for (int _d = 0; _d < D; _d++) {
						expr.addTerm(1, q2[i][d][_d][t]);

					}
				}
				model.addConstr(expr, GRB.LESS_EQUAL, f1[d], null);
			}
		}

		// Constraint 6 > 7

		for (int i = 0; i < I; i++) {
			for (int d = 0; d < D; d++) {
				for (int t = 0; t < T; t++) {

					expr = new GRBLinExpr();
					for (int _d = 0; _d < D; _d++) {
						expr.addTerm(1, q2[i][d][_d][t]);
					}
					expr.addTerm(-1, q0[i][d][t]);
					if (t > 0)
						expr.addTerm(-1, I1[i][d][t]);
					model.addConstr(expr, GRB.LESS_EQUAL, 0, null);

				}
			}
		}

		// Constraint 7 > 12

		for (int i = 0; i < I; i++) {
			for (int h = 0; h < H; h++) {
				for (int t = 0; t < T; t++) {

					expr = new GRBLinExpr();
					for (int d = 0; d < D; d++) {
						expr.addTerm(1, q1[i][d][h][t]);
					}
					expr.addTerm(1, B[i][h][t]);
					model.addConstr(expr, GRB.EQUAL, l[i][h][t], null);
				}
			}
		}

		// Constraint 8 > 14
		for (int i = 0; i < I; i++) {

			for (int t = 0; t < T; t++) {

				expr = new GRBLinExpr();
				for (int p = 0; p < P; p++) {
					expr.addTerm(1, Q0[i][p][t]);
					expr.addTerm(1, Q1[i][p][t]);
				}
				if (t > 0)
					expr.addTerm(1, I0[i][t - 1]);
				expr.addTerm(-1, I0[i][t]);
				for (int d = 0; d < D; d++) {
					expr.addTerm(-1, q0[i][d][t]);
				}
				model.addConstr(expr, GRB.EQUAL, 0, null);

			}
		}

		// Constraint 9 > 15
		for (int i = 0; i < I; i++) {
			for (int d = 0; d < D; d++) {
				for (int t = 0; t < T; t++) {

					expr = new GRBLinExpr();
					for (int _d = 0; _d < D; _d++) {
						expr.addTerm(1, q2[i][_d][d][t]);
						expr.addTerm(-1, q2[i][d][_d][t]);
					}
					expr.addTerm(1, q0[i][d][t]);
					if (t > 0)
						expr.addTerm(1, I1[i][d][t - 1]);
					expr.addTerm(-1, I1[i][d][t]);
					for (int h = 0; h < H; h++) {
						expr.addTerm(-1, q1[i][d][h][t]);
					}
					model.addConstr(expr, GRB.EQUAL, 0, null);

				}
			}
		}
		
		// setting intial inventory on had to 0
		expr = new GRBLinExpr();
		for (int i = 0; i < I; i++) {
			for (int d = 0; d < D; d++) {
				expr.addTerm(1, I1[i][d][0]);
			}
			expr.addTerm(1, I0[i][0]);
		}
		model.addConstr(expr, GRB.EQUAL, 0, null);


		// optimize model
		model.optimize();

		// print solution
		for (GRBVar var : model.getVars()) {
			if (var.get(GRB.DoubleAttr.X) > 0)
				System.out.println(var.get(GRB.StringAttr.VarName) + ": "
						+ var.get(GRB.DoubleAttr.X));
		}

		// calculate average amount of backordered demand
		double total = 0;
		int denuminator = 0;
		for (GRBVar var : model.getVars()) {
			if (var.get(GRB.StringAttr.VarName).contains("B")) {
				total += var.get(GRB.DoubleAttr.X);
				denuminator++;
			}
		}
		double output = total / denuminator;

		// dump model
		model.dispose();
		env.dispose();

		return output;
	}

	public static void main(String[] args) throws GRBException {
		double initialRatio = 0.05;
		double ratio;
		run(0.0);
		
		/* for (int i = 1; i <= itrNum; i++) { 
			 ratio = i * initialRatio;
			 System.out.println(ratio + "," + run(ratio)); 
			 }*/
		 
	}
}