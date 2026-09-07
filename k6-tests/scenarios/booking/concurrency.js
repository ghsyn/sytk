import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const created = new Counter('reservation_created');
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SEAT_ID = __ENV.SEAT_ID || 1;

export const options = {
    scenarios: {
        oversell: {
            executor: 'shared-iterations',
            startTime: '0s',
            gracefulStop: '10s',

            vus: 500,
            iterations: 1000,
            maxDuration: '30s',
        }
    }
    // , thresholds: { reservation_created: ['count<=1'] }     // After(분산락 적용 후): 초과판매 0 강제 검증
};

export default function () {
    const res = http.post(`${BASE_URL}/api/v1/reservations`,
        JSON.stringify({ userId: __VU, seatId: SEAT_ID }),
        { headers: { 'Content-Type': 'application/json' } });

    if (res.status === 201) created.add(1);   // 초과판매 검증
    check(res, {
        'created (201)': (r) => r.status === 201,
        'conflict (409)': (r) => r.status === 409,
    });
}
